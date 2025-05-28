let currentChatUserId = null;
const currentUserId = document.getElementById('currentUserId').value;
let chatUpdateInterval = null;

// Загрузка чата с пользователем
function loadChat(userId) {
    // Остановить предыдущий интервал обновления
    if (chatUpdateInterval) {
        clearInterval(chatUpdateInterval);
    }

    currentChatUserId = userId;
    updateActiveContactStyle(userId);
    fetchAndDisplayMessages(userId);

    // Запустить новый интервал обновления
    chatUpdateInterval = setInterval(() => {
        fetchAndDisplayMessages(userId);
    }, 5000); // Обновлять каждые 5 секунд
}

function updateActiveContactStyle(userId) {
    document.querySelectorAll('.contact-item').forEach(item => {
        item.classList.remove('active');
        if (item.getAttribute('data-userid') == userId) {
            item.classList.add('active');
            const username = item.querySelector('.contact-username').textContent;
            document.getElementById('chat-with-user').textContent = `Чат с ${username}`;
        }
    });
}

function fetchAndDisplayMessages(userId) {
    fetch(`/admin/messages/chat/${userId}`)
        .then(response => {
            if (!response.ok) throw new Error('Ошибка загрузки сообщений');
            return response.json();
        })
        .then(messages => {
            const messagesContainer = document.getElementById('chat-messages');
            const scrollPosition = messagesContainer.scrollTop;
            const wasScrolledToBottom =
                messagesContainer.scrollHeight - messagesContainer.clientHeight <= scrollPosition + 1;

            messagesContainer.innerHTML = '';

            messages.forEach(msg => {
                const messageDiv = document.createElement('div');
                messageDiv.className = `message ${msg.senderId == currentUserId ? 'sent' : 'received'}`;
                messageDiv.innerHTML = `
                        <p class="message-text">${msg.message}</p>
                        <p class="message-time">${formatMessageTime(msg.createdAt)}</p>
                    `;
                messagesContainer.appendChild(messageDiv);
            });

            if (wasScrolledToBottom) {
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            }
        })
        .catch(error => {
            console.error('Ошибка загрузки сообщений:', error);
        });
}

function formatMessageTime(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
}

// Инициализация чата при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Обработчик клика по контакту
    document.querySelectorAll('.contact-item').forEach(item => {
        item.addEventListener('click', function() {
            const userId = this.getAttribute('data-userid');
            loadChat(userId);
        });
    });

    // Инициализация первого чата (если есть контакты)
    const firstContact = document.querySelector('.contact-item');
    if (firstContact) {
        loadChat(firstContact.getAttribute('data-userid'));
    }

    // Обработчик отправки сообщения
    document.getElementById('send-btn').addEventListener('click', sendMessage);

    // Обработчик нажатия Enter в поле ввода
    document.getElementById('message-input').addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
});

function sendMessage() {
    if (!currentChatUserId) {
        alert('Выберите пользователя для чата');
        return;
    }

    const messageInput = document.getElementById('message-input');
    const message = messageInput.value.trim();

    if (!message) return;

    fetch('/admin/messages/send', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
        },
        body: JSON.stringify({
            recipientId: currentChatUserId,
            message: message
        })
    })
        .then(response => {
            if (!response.ok) throw new Error('Ошибка отправки сообщения');
            messageInput.value = '';
            fetchAndDisplayMessages(currentChatUserId); // Обновляем чат после отправки
        })
        .catch(error => {
            console.error('Ошибка отправки:', error);
            alert('Не удалось отправить сообщение');
        });
}

// Поиск пользователей
document.getElementById('user-search').addEventListener('input', function(e) {
    const searchTerm = e.target.value.toLowerCase();
    document.querySelectorAll('.contact-item').forEach(item => {
        const username = item.querySelector('.contact-username').textContent.toLowerCase();
        item.style.display = username.includes(searchTerm) ? 'flex' : 'none';
    });
});

// Очистка интервала при закрытии страницы
window.addEventListener('beforeunload', function() {
    if (chatUpdateInterval) {
        clearInterval(chatUpdateInterval);
    }
});