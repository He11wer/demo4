document.addEventListener('DOMContentLoaded', function() {
    const lotId = document.getElementById('lotId').value;
    const currentUserId = document.querySelector('input[name="currentUserId"]').value;

    // Функция для обновления чата
    function refreshChat() {
        fetch(`/user/lot/${lotId}/chat/messages`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
            }
        })
            .then(response => response.json())
            .then(messages => {
                const chatContainer = document.getElementById('chatMessages');
                if (chatContainer) {
                    chatContainer.innerHTML = messages.map(msg => `
                    <div class="chat-message">
                        <strong>${msg.user.username}</strong>:
                        <span>${msg.message}</span>
                        <small>${new Date(msg.createdAt).toLocaleString('ru-RU')}</small>
                    </div>
                `).join('');

                    // Прокручиваем чат вниз при новых сообщениях
                    chatContainer.scrollTop = chatContainer.scrollHeight;
                }
            })
            .catch(error => console.error('Ошибка при обновлении чата:', error));
    }

    // Функция для обновления отзывов
    function refreshReviews() {
        fetch(`/user/lot/${lotId}/ratings`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
            }
        })
            .then(response => response.json())
            .then(ratings => {
                const reviewsContainer = document.querySelector('.reviews-list');
                const emptyReviews = document.querySelector('.empty-reviews');

                if (ratings.length === 0) {
                    if (emptyReviews) emptyReviews.style.display = 'block';
                    if (reviewsContainer) reviewsContainer.style.display = 'none';
                } else {
                    if (emptyReviews) emptyReviews.style.display = 'none';
                    if (reviewsContainer) {
                        reviewsContainer.style.display = 'block';
                        reviewsContainer.innerHTML = ratings.map(rating => `
                        <div class="review-item">
                            <div class="review-header">
                                <span class="review-author">${rating.user.username}</span>
                                <div class="review-stars">
                                    ${Array.from({length: 5}, (_, i) =>
                            `<i class="${i < rating.rating ? 'fas' : 'far'} fa-star"></i>`
                        ).join('')}
                                </div>
                                <span class="review-date">${new Date(rating.createdAt).toLocaleString('ru-RU')}</span>
                            </div>
                            <div class="review-comment">${rating.comment || ''}</div>
                        </div>
                    `).join('');
                    }
                }

                // Обновляем средний рейтинг
                if (ratings.length > 0) {
                    const avgRating = ratings.reduce((sum, r) => sum + r.rating, 0) / ratings.length;
                    const ratingElement = document.querySelector('.product-rating span');
                    if (ratingElement) {
                        ratingElement.textContent = avgRating.toFixed(1);
                    }
                }
            })
            .catch(error => console.error('Ошибка при обновлении отзывов:', error));
    }

    // Функция для обновления ставок
    function refreshBids() {
        fetch(`/user/lot/${lotId}/current-bid`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
            }
        })
            .then(response => response.json())
            .then(data => {
                const priceElement = document.querySelector('.product-detail-price');
                if (priceElement) {
                    const price = data.lastBet !== null ? data.lastBet : data.startPrice;
                    priceElement.textContent = `Текущая цена: ${price} кредитов`;
                }

                // Обновляем статус аукциона
                const now = new Date();
                const startTime = new Date(data.startTime);
                const endTime = new Date(data.endTime);

                const bidMessageElements = document.querySelectorAll('.bid-message');
                if (bidMessageElements.length > 0) {
                    if (now < startTime) {
                        bidMessageElements[0].style.display = 'block';
                        bidMessageElements[0].innerHTML = `Аукцион начнется в ${startTime.toLocaleString('ru-RU')}`;
                        if (bidMessageElements.length > 1) bidMessageElements[1].style.display = 'none';
                    } else if (now > endTime) {
                        if (bidMessageElements.length > 0) bidMessageElements[0].style.display = 'none';
                        if (bidMessageElements.length > 1) {
                            bidMessageElements[1].style.display = 'block';
                            bidMessageElements[1].textContent = 'Аукцион завершен';
                        }
                    } else {
                        bidMessageElements.forEach(el => el.style.display = 'none');
                    }
                }
            })
            .catch(error => console.error('Ошибка при обновлении ставок:', error));
    }

    // Запускаем обновление чата каждые 3 секунды
    setInterval(refreshChat, 3000);

    // Запускаем обновление отзывов каждые 3 секунды
    setInterval(refreshReviews, 3000);

    // Запускаем обновление ставок каждые 3 секунды
    setInterval(refreshBids, 3000);

    // Первое обновление сразу после загрузки страницы
    refreshChat();
    refreshReviews();
    refreshBids();
});