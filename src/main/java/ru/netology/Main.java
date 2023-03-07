package ru.netology;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();
        // код инициализации сервера (из вашего предыдущего ДЗ)


        // добавление хендлеров (обработчиков)
//        server.addHandler(HttpMethod.GET, "/messages", new Handler() {
//            public void handle(Request request, BufferedOutputStream responseStream) {
//                // TODO: handlers code
//            }
//        });
//        server.addHandler(HttpMethod.POST "/messages", new Handler() {
//            public void handle(Request request, BufferedOutputStream responseStream) {
//                // TODO: handlers code
//            }
//        });

        server.fillHandlerList();//add 'get' handlers for all files in public folder
        server.listen(9999);
    }
}