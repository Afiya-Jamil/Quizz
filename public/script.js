const homeScreen = document.getElementById("home-screen");
const quizScreen = document.getElementById("quiz-screen");
const resultScreen = document.getElementById("result-screen");
const createScreen = document.getElementById("create-screen");

const homeMsg = document.getElementById("home-msg");
const quizList = document.getElementById("quiz-list");
const createBtn = document.getElementById("create-btn");

const progressEl = document.getElementById("progress");
const questionEl = document.getElementById("question");
const optionsEl = document.getElementById("options");

const scoreEl = document.getElementById("score");
const timeEl = document.getElementById("time");
const messageEl = document.getElementById("message");
const restartBtn = document.getElementById("restart-btn");
const homeBtn = document.getElementById("home-btn");

const titleInput = document.getElementById("quiz-title");
const questionBlocks = document.getElementById("question-blocks");
const addQuestionBtn = document.getElementById("add-question-btn");
const createError = document.getElementById("create-error");
const saveQuizBtn = document.getElementById("save-quiz-btn");
const cancelBtn = document.getElementById("cancel-btn");

let currentQuizId = 0;
let questions = [];
let current = 0;
let answers = [];
let startTime = 0;
let blockCounter = 0;

restartBtn.addEventListener("click", () => startQuiz(currentQuizId));
homeBtn.addEventListener("click", () => loadHome());
createBtn.addEventListener("click", openCreate);
addQuestionBtn.addEventListener("click", addQuestionBlock);
saveQuizBtn.addEventListener("click", saveQuiz);
cancelBtn.addEventListener("click", () => loadHome());

function showScreen(screen) {
  [homeScreen, quizScreen, resultScreen, createScreen].forEach(s => s.classList.add("hidden"));
  screen.classList.remove("hidden");
}

/* ---------- Home ---------- */

async function loadHome(message) {
  homeMsg.textContent = message || "";
  const res = await fetch("/api/quizzes");
  const quizzes = await res.json();

  quizList.innerHTML = "";
  quizzes.forEach(quiz => {
    const btn = document.createElement("button");
    btn.className = "option";
    btn.textContent = quiz.title + " (" + quiz.count + " questions)";
    btn.addEventListener("click", () => startQuiz(quiz.id));
    quizList.appendChild(btn);
  });

  showScreen(homeScreen);
}

/* ---------- Taking a quiz ---------- */

async function startQuiz(id) {
  currentQuizId = id;
  const res = await fetch("/api/questions?quiz=" + id);
  questions = await res.json();
  current = 0;
  answers = [];
  startTime = Date.now();
  showScreen(quizScreen);
  showQuestion();
}

function showQuestion() {
  const q = questions[current];
  progressEl.textContent = "Question " + (current + 1) + " of " + questions.length;
  questionEl.textContent = q.question;
  optionsEl.innerHTML = "";

  q.options.forEach((text, index) => {
    const btn = document.createElement("button");
    btn.className = "option";
    btn.textContent = text; // textContent keeps user text safe
    btn.addEventListener("click", () => selectAnswer(index));
    optionsEl.appendChild(btn);
  });
}

function selectAnswer(index) {
  answers.push(index);
  current++;
  if (current < questions.length) {
    showQuestion();
  } else {
    finishQuiz();
  }
}

async function finishQuiz() {
  const totalSeconds = Math.round((Date.now() - startTime) / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  const res = await fetch("/api/submit?quiz=" + currentQuizId, {
    method: "POST",
    body: answers.join(",")
  });
  const result = await res.json();

  scoreEl.textContent = "Correct answers: " + result.score + " / " + result.total;
  timeEl.textContent = "Time taken: " + minutes + "m " + seconds + "s";
  messageEl.textContent = result.passed ? "You passed!" : "You failed. Try again!";
  messageEl.className = result.passed ? "pass" : "fail";

  showScreen(resultScreen);
}

/* ---------- Make your own quiz ---------- */

function openCreate() {
  titleInput.value = "";
  createError.textContent = "";
  questionBlocks.innerHTML = "";
  blockCounter = 0;
  addQuestionBlock();
  showScreen(createScreen);
}

function addQuestionBlock() {
  if (questionBlocks.children.length >= 20) {
    createError.textContent = "Maximum 20 questions per quiz";
    return;
  }
  blockCounter++;
  const number = questionBlocks.children.length + 1;

  const block = document.createElement("div");
  block.className = "q-block";
  block.innerHTML = `
    <label>Question ${number}</label>
    <input type="text" class="q-text" placeholder="Type the question" maxlength="200">
    <p class="hint">Fill all 4 options and tick the correct one</p>
    ${[0, 1, 2, 3].map(j => `
      <div class="opt-row">
        <input type="radio" name="correct-${blockCounter}" value="${j}">
        <input type="text" class="opt-text" placeholder="Option ${j + 1}" maxlength="100">
      </div>`).join("")}
  `;
  questionBlocks.appendChild(block);
}

async function saveQuiz() {
  const blocks = questionBlocks.querySelectorAll(".q-block");

  const params = new URLSearchParams();
  params.append("title", titleInput.value.trim());
  params.append("count", blocks.length);

  blocks.forEach((block, i) => {
    params.append("q" + i, block.querySelector(".q-text").value.trim());
    block.querySelectorAll(".opt-text").forEach((input, j) => {
      params.append("q" + i + "o" + j, input.value.trim());
    });
    const checked = block.querySelector("input[type=radio]:checked");
    params.append("q" + i + "c", checked ? checked.value : "");
  });

  const res = await fetch("/api/create", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: params.toString()
  });
  const data = await res.json();

  if (!res.ok) {
    createError.textContent = data.error; // server tells what is missing
    return;
  }
  loadHome("Quiz saved! You can play it below.");
}

/* ---------- Start the app ---------- */
loadHome();