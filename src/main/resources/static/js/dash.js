function applyFilters() {
    const searchTerm = document.getElementById('search-input').value.toLowerCase();
    const categoryFilter = document.getElementById('filter-category').value;
    const sortBy = document.getElementById('sort-by').value;

    const productCards = Array.from(document.querySelectorAll('.product-card'));

    // Фильтрация
    productCards.forEach(card => {
        const title = card.querySelector('h3').textContent.toLowerCase();
        const description = card.querySelector('.product-description').textContent.toLowerCase();
        const categories = Array.from(card.querySelectorAll('.product-categories span'))
            .map(span => span.textContent.replace(',', '').trim());

        const matchesSearch = title.includes(searchTerm) || description.includes(searchTerm);
        const matchesCategory = categoryFilter === 'all' ||
            categories.some(cat => cat === categoryFilter);

        card.style.display = (matchesSearch && matchesCategory) ? 'block' : 'none';
    });

    // Сортировка
    const visibleCards = productCards.filter(card => card.style.display !== 'none');

    visibleCards.sort((a, b) => {
        const priceA = parseFloat(a.querySelector('.product-price').textContent.match(/\d+/)[0]);
        const priceB = parseFloat(b.querySelector('.product-price').textContent.match(/\d+/)[0]);
        const dateA = new Date(a.querySelector('.product-end-time').textContent.split(': ')[1]);
        const dateB = new Date(b.querySelector('.product-end-time').textContent.split(': ')[1]);

        switch(sortBy) {
            case 'price-asc': return priceA - priceB;
            case 'price-desc': return priceB - priceA;
            case 'date-old': return dateA - dateB;
            default: return dateB - dateA; // date-new по умолчанию
        }
    });

    // Переупорядочиваем карточки в DOM
    const productsGrid = document.getElementById('products-grid');
    visibleCards.forEach(card => productsGrid.appendChild(card));
}