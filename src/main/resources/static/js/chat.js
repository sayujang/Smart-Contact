//chat implementation

let stompClient = null;
let currentChatUserId = null; //the user the loggedin user is chatting to 
let currentChatUserName = null;
let currentLoggedInUserId = null; 
let typingTimeout = null; // Used for UI debounce


//websocket connection:
function connectWebSocket(userId) {
    // Prevent double connections
    if (stompClient !== null && stompClient.connected) {
        return;
    }

    //If userId is missing/null, do nothing.
    if (!userId) return;

    currentLoggedInUserId = userId;
    const socket = new SockJS('/ws'); //create  a socket
    stompClient = Stomp.over(socket); //wrap around stomp protocol
    stompClient.debug = null; // Hide logs to keep browser console clean


    //sends a stomp connect frame;
    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket as ' + userId);

        //matches my backend logic messagingTemplate.convertAndSend("/queue/messages/" + receiverId, ...)
        stompClient.subscribe('/queue/messages/' + userId, function(message) {
            const chatMessage = JSON.parse(message.body);
            
            //check for modal existence
            const modal = document.getElementById('chatModal');
            if (modal && !modal.classList.contains('hidden')) {
                displayMessage(chatMessage);
            } else {
                console.log("New message received in background");
            }
        });

        //subscribe to status updates (connected to addUser method in chat controller)
        stompClient.subscribe('/topic/user.status', function(message) { //every client subscribes to the same topic/user.status 
            const payload = JSON.parse(message.body);
            updateUserStatus(payload.userId, payload.status);
        });

        //subscribe to typing indicators (connected to handleTyping function in chat controller)
        stompClient.subscribe('/queue/typing/' + userId, function(message) {
             const data = JSON.parse(message.body);
             // Only show if we are currently looking at the user who is typing
             if (currentChatUserId && data.senderId === currentChatUserId) {
                 showTypingAnimation();
             }
        });

        //notify Server we are Online (connected to addUser method in chat controller)
        stompClient.send("/app/chat.connect", {}, JSON.stringify({ userId: userId }));

    }, function(error) {
        console.log('Chat connection failed (silent fail)');
    });
}

function disconnectWebSocket() {
    if (stompClient !== null) { //check if connection exists
        stompClient.send("/app/chat.disconnect", {}, JSON.stringify({
            userId: currentLoggedInUserId
        }));
        stompClient.disconnect();
    }
    console.log("Disconnected");
}

//only async function can have await statements
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
        const response = await fetch(url); //fetch always returns a promise. a promise object can have be resolved(success) or rejected(failure)
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
            await new Promise(resolve => setTimeout(resolve, 1000)); //waits for 1s as websocket connection can take time
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

        // Clear previous messages may be with another user
        document.getElementById('chatMessages').innerHTML = '';
        
        // Load chat history before model opens up
        await loadChatHistory(currentLoggedInUserId, currentChatUserId);
        
        // Check user status immediately before model opens up
        await checkUserStatus(currentChatUserId);
        
        // Show modal
        const modal = document.getElementById('chatModal');
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        
        // Focus on input
        document.getElementById('messageInput').focus();
        
    } catch (error) { //the failed promise is catched here
        console.error('Error opening chat:', error);
    }
}

function closeChatModal() {
    document.getElementById('chatModal').classList.add('hidden');
    document.getElementById('chatModal').classList.remove('flex');
    currentChatUserId = null;
    currentChatUserName = null;
}

async function loadChatHistory(userId1, userId2) {
    try {
        const response = await fetch(`/api/chat/history/${userId1}/${userId2}`);
        const messages = await response.json();
        messages.forEach(message => displayMessage(message, false));
        scrollToBottom(); //view recent messages
    } catch (error) { //catch the failed promise so that it doesn't crash
        console.error('Error loading chat history:', error);
    }
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim(); //removes spaces 
    
    if (!content || !stompClient || !stompClient.connected) return; //both stomp client and the websocket connections need to exists
    
    const chatMessage = {
        senderId: currentLoggedInUserId,
        receiverId: currentChatUserId,
        content: content,
        type: 'TEXT',
        timestamp: new Date().toISOString()
    };
    
    try {
        //send message to sendMessage function in backend
        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage)); //websockets can only transport strings
        input.value = '';
    } catch (error) {
        console.error('Error sending message:', error);
    }
}



// Handle Enter key press
function handleMessageKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

//Send Typing Signal
function handleTyping() {
    if (!stompClient || !currentChatUserId) return;
    
    // We just send the signal, we don't need complex timeout logic on the SENDER side
    stompClient.send("/app/chat.typing", {}, JSON.stringify({
        senderId: currentLoggedInUserId, 
        receiverId: currentChatUserId
    }));
}

// Receive Typing Signal & Toggle UI
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

//This function updates both the List Dot AND the Modal Text
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

document.addEventListener('DOMContentLoaded', function() {
    console.log('=== Chat System Initializing ===');
    
    // Connect WebSocket
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
async function handleFileUpload(inputElement) {
    const file = inputElement.files[0]; //files property is always a list for input type=file
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file); //matches requestparam in fileuploadcontroller

    try {
        // 1. Upload to Java Backend
        const response = await fetch('/api/chat/upload', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const data = await response.json();
            const fileUrl = data.url;
            const fileName = file.name; // Keep original name for display
            
            // 2. Determine Message Type
            const isImage = file.type.startsWith('image/');
            let messageContent = "";

            if (isImage) {
                // Format: IMG:URL
                messageContent = "IMG:" + fileUrl;
            } else {
                // Format: FILE:URL|OriginalName
                messageContent = "FILE:" + fileUrl + "|" + fileName;
            }

            // 3. Send via WebSocket
            sendFileMessage(messageContent); 
        } else {
            alert("Upload failed!");
        }
    } catch (error) {
        console.error("Error uploading file:", error);
    }
    
    // Reset input
    inputElement.value = ''; 
}

function sendFileMessage(contentString) {
    if (!stompClient || !stompClient.connected) return;

    const chatMessage = {
        senderId: currentLoggedInUserId,
        receiverId: currentChatUserId,
        content: contentString, 
        type: 'TEXT', // We keep type as TEXT and use the prefix to identify content
        timestamp: new Date().toISOString()
    };
    //send to sendmessage function in chatcontroller
    stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
    
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
    
    // --- CONTENT RENDERING LOGIC ---
    let contentHtml = '';
    const rawContent = message.content || "";

    if (rawContent.startsWith("IMG:")) {
        // SCENARIO 1: IMAGE
        const url = rawContent.substring(4);
        contentHtml = `
            <img src="${url}" 
                 class="max-w-[200px] rounded-lg cursor-pointer hover:opacity-90 transition" 
                 onclick="window.open(this.src, '_blank')">`;

    } else if (rawContent.startsWith("FILE:")) {
        // SCENARIO 2: GENERIC FILE
        // expected format: FILE:url|filename
        const parts = rawContent.substring(5).split("|");
        const url = parts[0];
        const fileName = parts.length > 1 ? parts[1] : "Download File";

        // Render a File Card
        contentHtml = `
            <a href="${url}" target="_blank" class="flex items-center space-x-3 p-1 hover:bg-black/10 dark:hover:bg-white/10 rounded transition group">
                <div class="bg-gray-100 dark:bg-gray-600 p-2 rounded text-gray-600 dark:text-gray-200">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                </div>
                <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium truncate underline group-hover:no-underline">${escapeHtml(fileName)}</p>
                    <p class="text-xs opacity-70">Click to download</p>
                </div>
            </a>`;
            
    } else {
        // SCENARIO 3: PLAIN TEXT
        contentHtml = `<p class="font-sans text-sm leading-relaxed break-words block">${escapeHtml(rawContent)}</p>`;
    }
    // -------------------------------

    messageDiv.innerHTML = `
        <div class="max-w-[80%] lg:max-w-[70%]">
            <div class="${isSender ? senderClasses : receiverClasses} px-4 py-2 shadow-sm">
                ${contentHtml}
            </div>
            <p class="text-[10px] text-gray-500 dark:text-gray-400 mt-1 ${isSender ? 'text-right' : 'text-left'}">${time}</p>
        </div>
    `;
    
    messagesDiv.appendChild(messageDiv);
    scrollToBottom();
}