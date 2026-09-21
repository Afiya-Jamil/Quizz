import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

    static class Question {
        String text;
        String[] options;
        int correct; // index of the right option

        Question(String text, String[] options, int correct) {
            this.text = text;
            this.options = options;
            this.correct = correct;
        }
    }

    static class Quiz {
        String title;
        List<Question> questions;

        Quiz(String title, List<Question> questions) {
            this.title = title;
            this.questions = questions;
        }
    }

    // All quizzes live here (in memory). Index in the list = quiz id.
    static final List<Quiz> QUIZZES = new CopyOnWriteArrayList<>();

    static {
        QUIZZES.add(new Quiz("Java, HTML and CSS", List.of(
            new Question("Which keyword creates an object in Java?",
                new String[]{"new", "create", "make", "object"}, 0),
            new Question("Which of these is NOT a primitive type in Java?",
                new String[]{"int", "boolean", "String", "char"}, 2),
            new Question("Which method is the entry point of a Java program?",
                new String[]{"start()", "main()", "run()", "init()"}, 1),
            new Question("Which keyword is used to inherit a class in Java?",
                new String[]{"implements", "extends", "inherits", "super"}, 1),
            new Question("Which of these does NOT allow duplicate elements?",
                new String[]{"ArrayList", "Set", "LinkedList", "Vector"}, 1),
            new Question("Which HTML tag creates a hyperlink?",
                new String[]{"<link>", "<a>", "<href>", "<url>"}, 1),
            new Question("Which HTML tag gives the largest heading?",
                new String[]{"<h6>", "<head>", "<h1>", "<header>"}, 2),
            new Question("Which attribute gives alternative text for an image?",
                new String[]{"src", "alt", "title", "href"}, 1),
            new Question("Which CSS property changes the text color?",
                new String[]{"font-color", "text-color", "color", "fgcolor"}, 2),
            new Question("Which CSS property adds space INSIDE an element's border?",
                new String[]{"margin", "padding", "spacing", "border-spacing"}, 1)
        )));
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 5000), 0);
        server.createContext("/", Main::serveFile);
        server.createContext("/api/quizzes", Main::handleQuizzes);
        server.createContext("/api/questions", Main::handleQuestions);
        server.createContext("/api/submit", Main::handleSubmit);
        server.createContext("/api/create", Main::handleCreate);
        server.start();
        System.out.println("Quizz running at http://127.0.0.1:5000/");
    }

    // GET /api/quizzes -> list of quizzes (id, title, number of questions)
    static void handleQuizzes(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendJson(exchange, 405, error("Use GET"));
            return;
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < QUIZZES.size(); i++) {
            Quiz quiz = QUIZZES.get(i);
            json.append("{\"id\":").append(i)
                .append(",\"title\":\"").append(escape(quiz.title))
                .append("\",\"count\":").append(quiz.questions.size()).append("}");
            if (i < QUIZZES.size() - 1) json.append(",");
        }
        json.append("]");
        sendJson(exchange, 200, json.toString());
    }

    // GET /api/questions?quiz=ID -> questions + options, NO correct answers
    static void handleQuestions(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendJson(exchange, 405, error("Use GET"));
            return;
        }
        Quiz quiz = getQuiz(exchange);
        if (quiz == null) {
            sendJson(exchange, 404, error("Quiz not found"));
            return;
        }
        List<Question> list = quiz.questions;
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Question q = list.get(i);
            json.append("{\"question\":\"").append(escape(q.text)).append("\",\"options\":[");
            for (int j = 0; j < q.options.length; j++) {
                json.append("\"").append(escape(q.options[j])).append("\"");
                if (j < q.options.length - 1) json.append(",");
            }
            json.append("]}");
            if (i < list.size() - 1) json.append(",");
        }
        json.append("]");
        sendJson(exchange, 200, json.toString());
    }

    // POST /api/submit?quiz=ID -> body like "0,2,1,1,3" (chosen option per question)
    static void handleSubmit(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendJson(exchange, 405, error("Use POST"));
            return;
        }
        Quiz quiz = getQuiz(exchange);
        if (quiz == null) {
            sendJson(exchange, 404, error("Quiz not found"));
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        String[] parts = body.split(",");

        int total = quiz.questions.size();
        int score = 0;
        for (int i = 0; i < total && i < parts.length; i++) {
            try {
                if (Integer.parseInt(parts[i].trim()) == quiz.questions.get(i).correct) {
                    score++;
                }
            } catch (NumberFormatException e) {
                // bad value counts as wrong
            }
        }
        int passMark = (total * 60 + 99) / 100; // 60% rounded up
        boolean passed = score >= passMark;
        sendJson(exchange, 200, "{\"score\":" + score + ",\"total\":" + total
                + ",\"passed\":" + passed + "}");
    }

    // POST /api/create -> form data: title, count, q0, q0o0..q0o3, q0c, q1, ...
    static void handleCreate(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendJson(exchange, 405, error("Use POST"));
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(body);

        String title = limit(form.getOrDefault("title", "").trim(), 80);
        if (title.isEmpty()) {
            sendJson(exchange, 400, error("Please enter a quiz title"));
            return;
        }

        int count;
        try {
            count = Integer.parseInt(form.getOrDefault("count", "0"));
        } catch (NumberFormatException e) {
            count = 0;
        }
        if (count < 1 || count > 20) {
            sendJson(exchange, 400, error("A quiz needs 1 to 20 questions"));
            return;
        }

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int n = i + 1;
            String text = limit(form.getOrDefault("q" + i, "").trim(), 200);
            if (text.isEmpty()) {
                sendJson(exchange, 400, error("Question " + n + " needs some text"));
                return;
            }

            String[] options = new String[4];
            for (int j = 0; j < 4; j++) {
                options[j] = limit(form.getOrDefault("q" + i + "o" + j, "").trim(), 100);
                if (options[j].isEmpty()) {
                    sendJson(exchange, 400, error("Question " + n + ": fill all 4 options"));
                    return;
                }
            }

            int correct;
            try {
                correct = Integer.parseInt(form.getOrDefault("q" + i + "c", ""));
            } catch (NumberFormatException e) {
                correct = -1;
            }
            if (correct < 0 || correct > 3) {
                sendJson(exchange, 400, error("Question " + n + ": choose the correct answer"));
                return;
            }

            questions.add(new Question(text, options, correct));
        }

        QUIZZES.add(new Quiz(title, questions));
        sendJson(exchange, 200, "{\"id\":" + (QUIZZES.size() - 1) + "}");
    }

    // reads ?quiz=ID from the URL
    static Quiz getQuiz(HttpExchange exchange) {
        Map<String, String> query = parseForm(exchange.getRequestURI().getRawQuery());
        try {
            int id = Integer.parseInt(query.getOrDefault("quiz", ""));
            if (id >= 0 && id < QUIZZES.size()) {
                return QUIZZES.get(id);
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        return null;
    }

    // turns "a=1&b=hello%20there" into a Map
    static Map<String, String> parseForm(String text) {
        Map<String, String> map = new HashMap<>();
        if (text == null || text.isEmpty()) return map;
        for (String pair : text.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    static String limit(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    static String error(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Serves files from the public/ folder
    static void serveFile(HttpExchange exchange) throws IOException {
        String urlPath = exchange.getRequestURI().getPath();
        if (urlPath.equals("/")) {
            urlPath = "/index.html";
        }

        Path publicDir = Path.of("public").toAbsolutePath().normalize();
        Path file = publicDir.resolve(urlPath.substring(1)).normalize();

        if (!file.startsWith(publicDir) || !Files.isRegularFile(file)) {
            byte[] msg = "404 Not Found".getBytes();
            exchange.sendResponseHeaders(404, msg.length);
            exchange.getResponseBody().write(msg);
            exchange.close();
            return;
        }

        byte[] data = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", getContentType(file.toString()));
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    static String getContentType(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }
}