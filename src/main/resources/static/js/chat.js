// chat.js - Pure JavaScript Chat Implementation

let stompClient = null;
let currentChatUserId = null;
let currentChatUserName = null;
let currentLoggedInUserId = null;
let typingTimeout = null; // Used for UI debounce

// ==========================================
// 1. WEBSOCKET CONNECTION
// ==========================================

function connectWebSocket(userId) {
    // Prevent double connections
    if (stompClient !== null && stompClient.connected) {
        return;
    }

    // SAFETY: If userId is missing/null, do nothing.
    if (!userId) return;

    currentLoggedInUserId = userId;
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Hide logs

    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket as ' + userId);

        // A. Subscribe to Messages
        stompClient.subscribe('/queue/messages/' + userId, function(message) {
            const chatMessage = JSON.parse(message.body);
            
            // Safe check for modal existence
            const modal = document.getElementById('chatModal');
            if (modal && !modal.classList.contains('hidden')) {
                displayMessage(chatMessage);
            } else {
                console.log("New message received in background");
            }
        });

        // B. Subscribe to Status Updates (Real-time Online/Offline)
        stompClient.subscribe('/topic/user.status', function(message) {
            const payload = JSON.parse(message.body);
            updateUserStatus(payload.userId, payload.status);
        });

        // C. [NEW] Subscribe to Typing Indicators
        stompClient.subscribe('/queue/typing/' + userId, function(message) {
             const data = JSON.parse(message.body);
             // Only show if we are currently looking at the user who is typing
             if (currentChatUserId && data.senderId === currentChatUserId) {
                 showTypingAnimation();
             }
        });

        // D. Notify Server we are Online
        stompClient.send("/app/chat.connect", {}, JSON.stringify({ userId: userId }));

    }, function(error) {
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

// ==========================================
// 2. MODAL & UI LOGIC
// ==========================================

async function openChatModal(contactEmail, contactName, contactId) {
    console.log('=== OPENING CHAT MODAL ===');
    
    // Get logged-in user ID if not set
    if (!currentLoggedInUserId) {
        const userIdElement = document.getElementById('loggedInUserId');
        if (userIdElement && userIdElement.value) {
            currentLoggedInUserId = userIdElement.value;
        } else {
            alert('Error: User session not found. Please refresh the page.');
            return;
        }
    }
    
    // Check if contact is a registered user
    try {
        const url = `/api/contact/is-user/${encodeURIComponent(contactEmail)}`;
        const response = await fetch(url);
        const data = await response.json();
        
        if (!data.isUser) {
            alert('This contact is not a registered user. Cannot start chat.\n\nEmail: ' + contactEmail);
            return;
        }
        
        currentChatUserId = data.userId;
        currentChatUserName = data.name;
        
        // Ensure WebSocket is connected
        if (!stompClient || !stompClient.connected) {
            connectWebSocket(currentLoggedInUserId);
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
        
        // Update modal header
        document.getElementById('chatUserName').textContent = data.name;
        document.getElementById('chatUserAvatar').src = data.profilePic || '/images/user.png';
        
        // Reset Status Text to "Checking..."
        const statusText = document.getElementById('chatUserStatus');
        statusText.textContent = '...';
        statusText.className = 'text-xs text-gray-300';
        
        // Hide typing indicator initially
        const typingInd = document.getElementById('headerTypingIndicator');
        if(typingInd) typingInd.classList.add('hidden');

        // Clear previous messages
        document.getElementById('chatMessages').innerHTML = '';
        
        // Load chat history
        await loadChatHistory(currentLoggedInUserId, currentChatUserId);
        
        // Check user status immediately
        await checkUserStatus(currentChatUserId);
        
        // Show modal
        const modal = document.getElementById('chatModal');
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        
        // Focus on input
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

// ==========================================
// 3. MESSAGE LOGIC
// ==========================================

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

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();
    
    if (!content || !stompClient || !stompClient.connected) return;
    
    const chatMessage = {
        senderId: currentLoggedInUserId,
        receiverId: currentChatUserId,
        content: content,
        type: 'TEXT',
        timestamp: new Date().toISOString()
    };
    
    try {
        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        input.value = '';
        // No need to explicitly stop typing, the UI timeout on the other end handles it
    } catch (error) {
        console.error('Error sending message:', error);
    }
}

function displayMessage(message, animate = true) {
    const messagesDiv = document.getElementById('chatMessages');
    const isSender = message.senderId === currentLoggedInUserId;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `flex ${isSender ? 'justify-end' : 'justify-start'} ${animate ? 'animate-fadeIn' : ''} mb-2`;
    
    const time = new Date(message.timestamp).toLocaleTimeString('en-US', {
        hour: '2-digit', minute: '2-digit'
    });
    
    const senderClasses = "bg-green-600 text-white rounded-t-lg rounded-bl-lg";
    const receiverClasses = "bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-800 dark:text-gray-100 rounded-t-lg rounded-br-lg";
    
    messageDiv.innerHTML = `
        <div class="max-w-[80%] lg:max-w-[70%]">
            <div class="${isSender ? senderClasses : receiverClasses} px-4 py-2 shadow-sm">
                <p class="font-sans text-sm leading-relaxed break-words block">${escapeHtml(message.content)}</p>
            </div>
            <p class="text-[10px] text-gray-500 dark:text-gray-400 mt-1 ${isSender ? 'text-right' : 'text-left'}">${time}</p>
        </div>
    `;
    
    messagesDiv.appendChild(messageDiv);
    scrollToBottom();
}

// ==========================================
// 4. STATUS & TYPING LOGIC
// ==========================================

// Handle Enter key press
function handleMessageKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
    // Note: handleTyping is now called via 'oninput' in HTML
}

// [MODIFIED] Send Typing Signal
function handleTyping() {
    if (!stompClient || !currentChatUserId) return;
    
    // We just send the signal, we don't need complex timeout logic on the SENDER side
    stompClient.send("/app/chat.typing", {}, JSON.stringify({
        senderId: currentLoggedInUserId, 
        receiverId: currentChatUserId
    }));
}

// [NEW] Receive Typing Signal & Toggle UI
function showTypingAnimation() {
    const indicator = document.getElementById('headerTypingIndicator'); // The header text
    const statusText = document.getElementById('chatUserStatus');       // The 'offline/online' text
    
    if (indicator) {
        indicator.classList.remove('hidden');
        if(statusText) statusText.classList.add('hidden'); // Hide status while typing

        // Reset the timeout
        if (typingTimeout) clearTimeout(typingTimeout);

        // Hide "typing..." after 2.5 seconds of silence
        typingTimeout = setTimeout(() => {
            indicator.classList.add('hidden');
            if(statusText) statusText.classList.remove('hidden');
        }, 2500);
    }
}


// Fetch single user status (used when opening modal)
async function checkUserStatus(userId) {
    try {
        const response = await fetch(`/api/chat/status/${userId}`);
        const data = await response.json();
        updateUserStatus(userId, data.status);
    } catch (error) {
        console.error('Error checking user status:', error);
    }
}

// CRITICAL: This function updates both the List Dot AND the Modal Text
// (PRESERVED FROM YOUR WORKING CODE)
function updateUserStatus(userId, status) {
    console.log(`Processing update -> User: ${userId}, Status: ${status}`);

    // 1. Find the Dot by the ID defined in your HTML
    const dotId = `status-dot-${userId}`;
    const statusDot = document.getElementById(dotId);

    if (statusDot) {
        // Remove BOTH color classes to be safe
        statusDot.classList.remove('bg-gray-400'); 
        statusDot.classList.remove('bg-green-500');

        // Clean the status string (trim whitespace/quotes just in case)
        const cleanStatus = String(status).trim().toUpperCase();

        if (cleanStatus === 'ONLINE') {
            statusDot.classList.add('bg-green-500'); // Turn Green
            statusDot.title = "Online";
        } else {
            statusDot.classList.add('bg-gray-400'); // Turn Gray
            statusDot.title = "Offline";
        }
    }

    // 2. Update Modal Text (if open)
    if (currentChatUserId && userId == currentChatUserId) {
        const statusText = document.getElementById('chatUserStatus');
        if (statusText) {
            const cleanStatus = String(status).trim().toUpperCase();
            if (cleanStatus === 'ONLINE') {
                statusText.textContent = 'online';
                statusText.className = 'text-xs text-green-100 font-bold'; // Kept your green-100 style
            } else {
                statusText.textContent = 'offline';
                statusText.className = 'text-xs text-green-100 font-bold';
            }
        }
    }
}

function scrollToBottom() {
    const messagesDiv = document.getElementById('chatMessages');
    if(messagesDiv) messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

function escapeHtml(text) {
    if(!text) return "";
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==========================================
// 5. INITIALIZATION
// ==========================================

// (PRESERVED EXACTLY TO ENSURE DOTS WORK)
document.addEventListener('DOMContentLoaded', function() {
    console.log('=== Chat System Initializing ===');
    
    // 1. Connect WebSocket
    const userIdElement = document.getElementById('loggedInUserId');
    if (userIdElement && userIdElement.value) {
        currentLoggedInUserId = userIdElement.value;
        connectWebSocket(currentLoggedInUserId);
    }

    // 2. Map Contact Emails to User IDs and Check Status
    const dots = document.querySelectorAll('.user-status-dot');
    console.log(`Found ${dots.length} contacts. Resolving User IDs...`);

    dots.forEach(dot => {
        const email = dot.getAttribute('data-email');
        if (email) {
            // API Call: Get User ID from Email
            fetch(`/api/contact/is-user/${encodeURIComponent(email)}`)
                .then(response => response.json())
                .then(data => {
                    if (data.isUser && data.userId) {
                        // CRITICAL: Assign the correct User ID to the DOM element
                        // Now the WebSocket updates will match this ID!
                        dot.id = `status-dot-${data.userId}`;
                        
                        // Check initial status
                        return fetch(`/api/chat/status/${data.userId}`);
                    } else {
                        throw new Error("Not a registered user");
                    }
                })
                .then(response => {
                     // Check if response is JSON or Text
                     const contentType = response.headers.get("content-type");
                     if (contentType && contentType.includes("application/json")) {
                         return response.json().then(d => d.status);
                     } else {
                         return response.text();
                     }
                })
                .then(status => {
                    // Extract ID back from the dot we just modified
                    const userId = dot.id.replace('status-dot-', '');
                    updateUserStatus(userId, status);
                })
                .catch(err => {
                    // Silent fail is fine (contact might not be a registered user)
                });
        }
    });
});

// Clean disconnect
window.addEventListener('beforeunload', function() {
    disconnectWebSocket();
});