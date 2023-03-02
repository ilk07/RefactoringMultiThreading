# Шаблоны проектирования

## Magics, DRY, SOLID

### Магазин


![Онлайн магазин](https://www.pngitem.com/pimgs/m/522-5229044_e-commerce-store-png-transparent-png.png "Онлайн магазин")

 Основной функционал:
 - просмотр каталога товаров
 - сортировка товаров по названию
 - добавление в корзину
 - оформление заказа



Магические числа = не используем "магия", например, при построении меню действий пользователя, нумеруем с помощью счётчика ([printShopActions()](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/PrintService.java))

SOLID
S = Single Responsibility Principle реализован в классах:
- [Shop](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Shop.java)
- [Product](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Product.java)
- [Device](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Device.java)
  
O = Open closed Principle реализован 
- в интерфейсе [Notifier](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Notifier.java) 
- в классе [EmailNotification](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/EmailNotification.java)

L = Liskov substitution Principle реализован в классах 
- [Product](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Product.java)
- [Device](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Device.java)
/ дополняет родителя (Product), не подменяя его

I = Interface Segregation Principle реализован в классах 
- [Order](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Order.java)
- [EmailNotification](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/EmailNotification.java)

D = Dependency Inversion Principle условно увидим в интерфейсе [Remains](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/Remains.java) (остатки товара). 

 *Пока не знаем какая именно будет реализация учёта остатков товаров, но уже есть интерфейс через который сможем SOLIDно дополнять в будущем*

DRY = реализован в классе [PrintService](https://github.com/ilk07/OnlineShop/blob/main/src/main/java/hw/PrintService.java), даёт возможность не повторять код для печати сообщений или списков, а переиспользовать его