// Переключение между секциями профиля
document.querySelectorAll('.profile-nav-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        const section = this.dataset.section;

        // Удаляем активный класс у всех кнопок и секций
        document.querySelectorAll('.profile-nav-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.profile-section').forEach(s => s.classList.remove('active'));

        // Добавляем активный класс текущей кнопке и соответствующей секции
        this.classList.add('active');
        document.getElementById(`${section}-section`).classList.add('active');
    });
});