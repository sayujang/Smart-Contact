let stompClient = null;
let currentChatUserId = null; 
let currentChatUserName = null;
let currentLoggedInUserId = null;
let typingTimeout = null; 

// --- WEBSOCKET CONNECTION ---
function connectWebSocket(userId) {
    if (stompClient !== null && stompClient.connected) {
        return;
    }

    if (!userId) return;

    const tokenInput = document.getElementById("userJwt");
    const token = tokenInput ? tokenInput.value : null;
    if (!token) {
        console.error("❌ Fatal Error: JWT Token not found.");
        return;
    }
    currentLoggedInUserId = userId;
    const socket = new SockJS('/ws'); 
    stompClient = Stomp.over(socket); 
    stompClient.debug = null; 

    const headers = {
        'Authorization': 'Bearer ' + token
    };

    stompClient.connect(headers, function (frame) {
        console.log('Connected to WebSocket as ' + userId);

        // 1. Subscribe to Messages
        stompClient.subscribe('/queue/messages/' + userId, function (message) {
            const chatMessage = JSON.parse(message.body);

            // FIX 1: This logic MUST be inside the subscription callback
            if (chatMessage.senderId === currentLoggedInUserId) {
                // Find and remove pending messages (Optimistic UI cleanup)
                const pendingMsgs = document.querySelectorAll('[id^="temp-"]');
                if (pendingMsgs.length > 0) {
                    pendingMsgs[pendingMsgs.length - 1].remove(); 
                }
            }

            const modal = document.getElementById('chatModal');
            if (modal && !modal.classList.contains('hidden')) {
                displayMessage(chatMessage);
            }
        });

        // 2. Subscribe to Read Receipts
        stompClient.subscribe('/queue/read/' + userId, function (message) {
            const data = JSON.parse(message.body);
            if (currentChatUserId && data.receiverId === currentChatUserId) {
                markAllMessagesAsSeenUI(); 
            }
        });

        // 3. Subscribe to Status Updates
        stompClient.subscribe('/queue/status/' + userId, function (message) {
            const payload = JSON.parse(message.body);
            updateUserStatus(payload.userId, payload.status);
        });

        // 4. Subscribe to Typing
        stompClient.subscribe('/queue/typing/' + userId, function (message) {
            const data = JSON.parse(message.body);
            if (currentChatUserId && data.senderId === currentChatUserId) {
                showTypingAnimation();
            }
        });

        // Notify Server we are Online
        stompClient.send("/app/chat.connect", {}, JSON.stringify({ userId: userId }));

    }, function (error) {
        console.log('Chat connection failed (silent fail)');
    });
}

function disconnectWebSocket() {
    if (stompClient !== null) { 
        stompClient.send("/app/chat.disconnect", {}, JSON.stringify({
            userId: currentLoggedInUserId
        }));
        stompClient.disconnect();
    }
    console.log("Disconnected");
}

// --- MODAL & UI LOGIC ---

async function openChatModal(contactEmail, contactName, contactId, isUnknown = false) {
    console.log('=== OPENING CHAT MODAL ===');

    const addBtn = document.getElementById('btnAddUnknown');
    if (addBtn) {
        if (isUnknown) {
            addBtn.classList.remove('hidden');
            addBtn.href = `/user/contact/add?name=${encodeURIComponent(contactName)}&email=${encodeURIComponent(contactEmail)}`;
        } else {
            addBtn.classList.add('hidden');
        }
    }
    
    if (!currentLoggedInUserId) {
        const userIdElement = document.getElementById('loggedInUserId');
        if (userIdElement && userIdElement.value) {
            currentLoggedInUserId = userIdElement.value;
        } else {
            return;
        }
    }

    try {
        const url = `/api/contact/is-user/${encodeURIComponent(contactEmail)}`;
        const response = await fetch(url); 
        const data = await response.json();

        if (!data.isUser) {
            alert('This contact is not a registered user.');
            return;
        }

        currentChatUserId = data.userId;
        currentChatUserName = data.name;

        // Ensure WebSocket is connected
        if (!stompClient || !stompClient.connected) {
            connectWebSocket(currentLoggedInUserId);
            await new Promise(resolve => setTimeout(resolve, 1000)); 
        }

        // UI Updates
        document.getElementById('chatUserName').textContent = data.name;
        document.getElementById('chatUserAvatar').src = data.profilePic || '/images/user.png';
        const typingInd = document.getElementById('headerTypingIndicator');
        if (typingInd) typingInd.classList.add('hidden');
        document.getElementById('chatMessages').innerHTML = '';

        // Load Data
        await loadChatHistory(currentLoggedInUserId, currentChatUserId);
        sendReadReceipt(currentChatUserId); // Mark history as read
        await checkUserStatus(currentChatUserId);

        const modal = document.getElementById('chatModal');
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        document.getElementById('messageInput').focus();

    } catch (error) { 
        console.error('Error opening chat:', error);
    }
}

function closeChatModal() {
    document.getElementById('chatModal').classList.add('hidden');
    document.getElementById('chatModal').classList.remove('flex');
    currentChatUserId = null;
    currentChatUserName = null;
}

// --- MESSAGE SENDING & DISPLAY ---

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content) return; 
    
    const tempId = "temp-" + Date.now();
    
    // Create temp message object
    const tempMessage = {
        senderId: currentLoggedInUserId,
        receiverId: currentChatUserId,
        content: content,
        type: 'TEXT',
        timestamp: new Date().toISOString(),
        status: 'PENDING' 
    };

    // Render immediately (Optimistic UI)
    displayMessage(tempMessage, true, tempId);
    
    input.value = '';

    if (stompClient && stompClient.connected) {
        try {
            const chatMessage = {
                senderId: currentLoggedInUserId,
                receiverId: currentChatUserId,
                content: content,
                type: 'TEXT',
                timestamp: new Date().toISOString()
            };
            
            stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        } catch (error) {
            console.error("Send failed", error);
            markMessageAsFailed(tempId);
        }
    } else {
        console.error("No connection");
        markMessageAsFailed(tempId);
    }
}

// FIX 2: Added tempId as the third argument here
function displayMessage(message, animate = true, tempId = null) {
    const messagesDiv = document.getElementById('chatMessages');
    const isSender = message.senderId === currentLoggedInUserId;
    
    // If tempId is provided use it, otherwise use message ID from DB
    // Note: message.id might be null for temp messages, that's fine
    const messageElementId = tempId || `msg-${message.id}`;
    
    let statusIcon = '';
    
    if (isSender) {
        if (message.status === 'PENDING') {
            statusIcon = `<i class="fa-regular fa-clock text-[10px] text-gray-400" title="Sending..."></i>`;
        } else if (message.status === 'FAILED') {
            statusIcon = `<i class="fa-solid fa-circle-exclamation text-[10px] text-red-500 cursor-pointer" title="Failed to send. Click to retry."></i>`;
        } else {
            const isSeen = message.status === 'SEEN';
            const tickColor = isSeen ? 'text-blue-500' : 'text-gray-400';
            statusIcon = `<i class="fa-solid fa-check-double text-[10px] ${tickColor} message-tick-icon"></i>`;
        }
    }

    const messageDiv = document.createElement('div');
    if (tempId) messageDiv.id = tempId;
    
    messageDiv.className = `flex ${isSender ? 'justify-end' : 'justify-start'} ${animate ? 'animate-fadeIn' : ''} mb-2`;

    const time = new Date(message.timestamp).toLocaleTimeString('en-US', {
        hour: '2-digit', minute: '2-digit'
    });

    const senderClasses = "bg-green-600 text-white rounded-t-lg rounded-bl-lg";
    const receiverClasses = "bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-800 dark:text-white rounded-t-lg rounded-br-lg";

    // --- CONTENT RENDERING ---
    let contentHtml = '';
    const rawContent = message.content || "";

    if (rawContent.startsWith("IMG:")) {
        const url = rawContent.substring(4);
        contentHtml = `
            <img src="${url}" 
                 class="max-w-[200px] rounded-lg cursor-pointer hover:opacity-90 transition" 
                 onclick="window.open(this.src, '_blank')">`;
    } else if (rawContent.startsWith("FILE:")) {
        const parts = rawContent.substring(5).split("|");
        const url = parts[0];
        const fileName = parts.length > 1 ? parts[1] : "Download File";

        contentHtml = `
            <a href="${url}" target="_blank" class="flex items-center space-x-3 p-1 hover:bg-black/10 dark:hover:bg-white/10 rounded transition group">
                <div class="bg-gray-100 dark:bg-gray-600 p-2 rounded text-gray-600 dark:text-gray-200">
                    <i class="fa-solid fa-file w-6 h-6"></i>
                </div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium truncate underline group-hover:no-underline">${escapeHtml(fileName)}</p>
                    <p class="text-xs opacity-70">Click to download</p>
                </div>
            </a>`;
    } else {
        contentHtml = `<p class="font-sans text-sm leading-relaxed break-words block">${escapeHtml(rawContent)}</p>`;
    }

    
    messageDiv.innerHTML = `
        <div class="max-w-[80%] lg:max-w-[70%]">
            <div class="${isSender ? senderClasses : receiverClasses} px-4 py-2 shadow-sm">
                ${contentHtml}
            </div>
            <div class="flex items-center justify-${isSender ? 'end' : 'start'} mt-1 space-x-1">
                <p class="text-[10px] text-gray-500 dark:text-white">${time}</p>
                <span class="ml-1 message-status-icon">
                    ${statusIcon}
                </span>
            </div>
        </div>
    `;

    messagesDiv.appendChild(messageDiv);
    scrollToBottom();
    
    // Read Receipt Logic for Receiver
    if (!isSender && currentChatUserId === message.senderId) {
        sendReadReceipt(message.senderId);
    }
}

function handleMessageKeyPress(event) {
    if (event.key === 'Enter') sendMessage();
}

function markMessageAsFailed(tempId) {
    const msgDiv = document.getElementById(tempId);
    if (msgDiv) {
        const iconSpan = msgDiv.querySelector('.message-status-icon');
        if (iconSpan) {
            iconSpan.innerHTML = `<i class="fa-solid fa-circle-exclamation text-[10px] text-red-500" title="Network Error"></i>`;
        }
    }
}

// --- HELPER FUNCTIONS ---

function sendReadReceipt(senderId) {
    if (!stompClient || !stompClient.connected) return;
    
    stompClient.send("/app/chat.read", {}, JSON.stringify({
        senderId: senderId, 
        receiverId: currentLoggedInUserId 
    }));
}

function markAllMessagesAsSeenUI() {
    const ticks = document.querySelectorAll('.message-tick-icon');
    ticks.forEach(tick => {
        tick.classList.remove('text-gray-400');
        tick.classList.remove('fa-check'); // in case it was single tick
        tick.classList.add('text-blue-500');
    });
}

async function loadChatHistory(userId1, userId2) {
    try {
        const response = await fetch(`/api/chat/history/${userId1}/${userId2}`);
        const messages = await response.json();
        messages.forEach(message => displayMessage(message, false));
        scrollToBottom(); 
    } catch (error) { 
        console.error('Error loading chat history:', error);
    }
}

function handleTyping() {
    if (!stompClient || !currentChatUserId) return;
    stompClient.send("/app/chat.typing", {}, JSON.stringify({
        senderId: currentLoggedInUserId,
        receiverId: currentChatUserId
    }));
}

function showTypingAnimation() {
    const indicator = document.getElementById('headerTypingIndicator'); 
    const statusText = document.getElementById('chatUserStatus');       

    if (indicator) {
        indicator.classList.remove('hidden');
        if (statusText) statusText.classList.add('hidden'); 

        if (typingTimeout) clearTimeout(typingTimeout);

        typingTimeout = setTimeout(() => {
            indicator.classList.add('hidden');
            if (statusText) statusText.classList.remove('hidden');
        }, 2500);
    }
}

async function checkUserStatus(userId) {
    try {
        const response = await fetch(`/api/chat/status/${userId}`);
        const data = await response.json();
        updateUserStatus(userId, data.status);
    } catch (error) {
        console.error('Error checking user status:', error);
    }
}

function updateUserStatus(userId, status) {
    const dotId = `status-dot-${userId}`;
    const statusDot = document.getElementById(dotId);

    if (statusDot) {
        statusDot.classList.remove('bg-gray-400');
        statusDot.classList.remove('bg-green-500');

        const cleanStatus = String(status).trim().toUpperCase();

        if (cleanStatus === 'ONLINE') {
            statusDot.classList.add('bg-green-500'); 
            statusDot.title = "Online";
        } else {
            statusDot.classList.add('bg-gray-400'); 
            statusDot.title = "Offline";
        }
    }

    if (currentChatUserId && userId == currentChatUserId) {
        const statusText = document.getElementById('chatUserStatus');
        if (statusText) {
            const cleanStatus = String(status).trim().toUpperCase();
            if (cleanStatus === 'ONLINE') {
                statusText.textContent = 'online';
                statusText.className = 'text-xs text-green-100 font-bold'; 
            } else {
                statusText.textContent = 'offline';
                statusText.className = 'text-xs text-green-100 font-bold';
            }
        }
    }
}

function scrollToBottom() {
    const messagesDiv = document.getElementById('chatMessages');
    if (messagesDiv) messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

function escapeHtml(text) {
    if (!text) return "";
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// --- INITIALIZATION ---

document.addEventListener('DOMContentLoaded', function () {
    console.log('=== Chat System Initializing ===');

    const userIdElement = document.getElementById('loggedInUserId');
    if (userIdElement && userIdElement.value) {
        currentLoggedInUserId = userIdElement.value;
        connectWebSocket(currentLoggedInUserId);
    }

    const dots = document.querySelectorAll('.user-status-dot');
    dots.forEach(dot => {
        const email = dot.getAttribute('data-email');
        if (email) {
            fetch(`/api/contact/is-user/${encodeURIComponent(email)}`)
                .then(response => response.json())
                .then(data => {
                    if (data.isUser && data.userId) {
                        dot.id = `status-dot-${data.userId}`;
                        return fetch(`/api/chat/status/${data.userId}`);
                    }
                })
                .then(response => {
                     if (!response) return;
                     const contentType = response.headers.get("content-type");
                     if (contentType && contentType.includes("application/json")) {
                         return response.json().then(d => d.status);
                     } else {
                         return response.text();
                     }
                })
                .then(status => {
                    if(status && dot.id) {
                        const userId = dot.id.replace('status-dot-', '');
                        updateUserStatus(userId, status);
                    }
                })
                .catch(err => {});
        }
    });
});

window.addEventListener('beforeunload', function () {
    disconnectWebSocket();
});

// File Upload Logic
async function handleFileUpload(inputElement) {
    const file = inputElement.files[0]; 
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file); 

    try {
        const response = await fetch('/api/chat/upload', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const data = await response.json();
            const fileUrl = data.url;
            const fileName = file.name; 

            const isImage = file.type.startsWith('image/');
            let messageContent = "";

            if (isImage) {
                messageContent = "IMG:" + fileUrl;
            } else {
                messageContent = "FILE:" + fileUrl + "|" + fileName;
            }

            sendFileMessage(messageContent);
        } else {
            alert("Upload failed!");
        }
    } catch (error) {
        console.error("Error uploading file:", error);
    }
    inputElement.value = '';
}

function sendFileMessage(contentString) {
    if (!stompClient || !stompClient.connected) return;

    const chatMessage = {
        senderId: currentLoggedInUserId,
        receiverId: currentChatUserId,
        content: contentString,
        type: 'TEXT', 
        timestamp: new Date().toISOString()
    };
    stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
}