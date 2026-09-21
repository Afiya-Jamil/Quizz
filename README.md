# Quizz
A beginner friendly quiz web app with a Java backend and a plain HTML, CSS and JavaScript frontend. It runs entirely on your own computer (localhost) with no frameworks and no external libraries.

Take the built-in quiz on Java, HTML and CSS, or make your own quiz and play it right away.

Table of Contents
Features
Tech Stack
Project Structure
Prerequisites
How to Run
How to Use
How It Works
API Reference
Limits and Validation
Troubleshooting
Sharing the App Online
Known Limitations
Future Improvements
Author
Features
Start a quiz with one click
One question at a time, with 4 answer options
Automatically moves to the next question after you pick an answer
Result screen showing:
Number of correct answers
Time taken to finish the quiz
Pass or fail message (pass mark is 60%)
Make your own quiz: add a title, up to 20 questions, 4 options each, and mark the correct answer
Home screen that lists every available quiz (built in and user made)
Correct answers are checked on the server, so they are never sent to the browser
Works on phone screens too (responsive design)
Tech Stack
Part	Technology
Backend	Java, using the built in com.sun.net.httpserver.HttpServer
Frontend	HTML, CSS, vanilla JavaScript
Data exchange	JSON (built with basic string formatting)
External libraries	None
Frameworks	None (no Spring Boot)
Project Structure
## Project Structure

```
Quizz/
├── Main.java
├── README.md
└── public/
    ├── index.html
    ├── style.css
    └── script.js
```

Main.java must sit in the project root, next to the public/ folder. The server looks for public/ relative to the folder you run it from.

Prerequisites
JDK 11 or higher. The app is started with java Main.java, which runs a single source file directly and needs Java 11+.
A modern web browser (Chrome, Edge, Firefox).

Check your Java version:

java -version

If the command is not recognized, install a JDK (for example Temurin/OpenJDK) and make sure it is added to your PATH.

You do not need Node.js. script.js runs inside the browser, not in the terminal.

How to Run
Open a terminal inside the project folder (the folder that contains Main.java). In VS Code: open the folder, then Terminal → New Terminal.
Start the server:
   java Main.java
Wait for this message:
   Quizz running at http://127.0.0.1:5000/
Open http://127.0.0.1:5000/ in your browser.
Keep the terminal open while using the app. Press Ctrl + C to stop the server.

Changed Main.java? Stop the server and run it again. Changed index.html, style.css or script.js? Just refresh the browser (use Ctrl + F5 if the old version keeps showing).

How to Use
Take a quiz
On the home screen, click a quiz.
Click one answer for each question. The next question appears automatically.
After the last question, see your score, your time, and a pass or fail message.
Click Try Again to retake the same quiz, or Home to pick another.
Make your own quiz
On the home screen, click + Make your own quiz.
Enter a quiz title.
For each question, type the question and fill in all 4 options, then tick the radio button next to the correct one.
Click + Add question for more questions (up to 20).
Click Save quiz. It appears on the home screen straight away, ready to play.

If something is missing, the app shows a message telling you exactly what to fix.

How It Works
Browser (script.js)  <--- JSON over HTTP --->  Java server (Main.java)
The browser loads index.html, style.css and script.js from the server.
script.js asks the server for the list of quizzes and shows them on the home screen.
When you pick a quiz, the browser asks for its questions. The server sends the questions and options only, without the correct answers.
The browser records the option number you picked for each question and measures the time taken.
At the end, the browser sends your picks to the server. The server compares them with the correct answers and replies with the score and pass or fail.
When you create a quiz, the form data is sent to the server, validated, and added to the in-memory list of quizzes.

Pass mark: 60% of the total questions, rounded up. For example, a 10-question quiz needs 6 correct, and a 3-question quiz needs 2.

Static files: the server serves anything inside public/. It also blocks requests that try to escape that folder (for example ../Main.java).

API Reference

All responses are JSON. Base URL: http://127.0.0.1:5000

GET /api/quizzes

Returns every quiz.

json
[
  { "id": 0, "title": "Java, HTML and CSS", "count": 10 }
]
GET /api/questions?quiz=ID

Returns the questions of one quiz. Correct answers are not included.

json
[
  {
    "question": "Which keyword creates an object in Java?",
    "options": ["new", "create", "make", "object"]
  }
]

Returns 404 if the quiz does not exist.

POST /api/submit?quiz=ID

Body (plain text): the chosen option index for each question, separated by commas.

0,2,1,1,3,1,2,1,2,1

Response:

json
{ "score": 8, "total": 10, "passed": true }

Invalid values in the body count as wrong answers.

POST /api/create

Content-Type: application/x-www-form-urlencoded

Field	Meaning
title	Quiz title
count	Number of questions
q0, q1, ...	Question text
q0o0 ... q0o3	The 4 options of question 0 (same pattern for other questions)
q0c, q1c, ...	Index (0 to 3) of the correct option

Success:

json
{ "id": 1 }

Failure (status 400):

json
{ "error": "Question 2: fill all 4 options" }

Wrong HTTP method on any endpoint returns 405.

Limits and Validation
Rule	Limit
Quiz title	Required, max 80 characters
Questions per quiz	1 to 20
Question text	Required, max 200 characters
Options per question	Exactly 4, each required, max 100 characters
Correct answer	Must be chosen for every question

All checks happen on the server, even though the form also guides you in the browser.

Troubleshooting
Problem	Fix
node is not recognized	You don't need Node. Run java Main.java instead.
java is not recognized	Install a JDK (11 or higher) and add it to PATH. Then reopen the terminal.
404 Not Found or blank page	Run the command from the folder that contains Main.java and public/.
Address already in use	Another program (or an old copy of this server) is using port 5000. Stop it, or change 5000 in Main.java to another port such as 5001.
Old design or behaviour still showing	Hard refresh with Ctrl + F5.
Changes to Main.java not working	Stop the server (Ctrl + C) and start it again.
My quiz disappeared	User-made quizzes are stored in memory and are lost when the server stops (see limitations).
Cannot open from another device	The server only listens on 127.0.0.1 (this computer). See the next section.
Sharing the App Online

The server runs on your laptop, so it cannot be hosted on static hosts like GitHub Pages. To let someone else try it while your server is running, use a tunnel tool such as ngrok:

ngrok http 5000

It gives you a public link that forwards to your local server. The link works only while both the server and the tunnel are running.

Known Limitations
User-made quizzes are kept in memory only, so they are lost when the server stops.
All users share the same list of quizzes, and anyone can add one.
The time taken is measured in the browser, so it is not tamper-proof.
There are no accounts and no saved score history.
Answers are matched by position, so the question order must not change between loading a quiz and submitting it.
Future Improvements
Save user-made quizzes to a file (for example JSON) so they survive restarts
Save high scores and attempt history
Shuffle question and option order
Add categories and difficulty levels
Share results on social media
Load extra questions from the Open Trivia Database API
User accounts with a personal dashboard
Edit and delete quizzes


