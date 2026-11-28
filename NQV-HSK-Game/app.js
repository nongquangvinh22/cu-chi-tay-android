(function () {
  const DATA = window.NQV_DATA;
  const state = {
    view: "home",
    profile: null,
    progress: null,
    lesson: null,
    lessonIndex: 0,
    lessonAnswered: false,
    quiz: null,
    quizIndex: 0,
    quizAnswers: [],
    selectedOfficial: "",
    authMode: "register"
  };
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));
  const shuffle = (items) => items.map((value) => ({ value, sort: Math.random() })).sort((a, b) => a.sort - b.sort).map((entry) => entry.value);
  const allWords = DATA.stages.flatMap((stage) => stage.lessons.flatMap((lesson) => lesson.words));
  const stageById = (id) => DATA.stages.find((stage) => stage.id === id);
  const lessonById = (id) => DATA.stages.flatMap((stage) => stage.lessons).find((lesson) => lesson.id === id);
  const escapeHTML = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]));
  const passedCount = () => state.progress ? state.progress.passedExams.length : 0;
  const currentStageIndex = () => Math.min(passedCount(), DATA.stages.length - 1);
  const stageUnlocked = (index) => index <= passedCount();
  const lessonsForStageDone = (stage) => stage.lessons.every((lesson) => state.progress.completedLessons.includes(lesson.id));
  const totalLessons = DATA.stages.reduce((count, stage) => count + stage.lessons.length, 0);
  const completedLessonCount = () => state.progress.completedLessons.length;
  const rankName = () => passedCount() === 0 ? "Học trò nhập môn" : DATA.ranks[Math.min(passedCount() - 1, DATA.ranks.length - 1)];
  const rankGlyph = () => passedCount() === 0 ? "童" : DATA.stages[Math.min(passedCount() - 1, 5)].glyph;

  function showToast(message) {
    const toast = $("#toast");
    toast.textContent = message;
    toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2600);
  }
  function shuffleChoices(correct, scope = allWords) {
    const choices = [correct];
    const distractors = shuffle(scope.filter((word) => word.meaning !== correct.meaning)).slice(0, 3);
    distractors.forEach((word) => choices.push(word));
    return shuffle(choices);
  }
  function updateUserInterface() {
    if (!state.profile || !state.progress) return;
    $("#xp-value").textContent = state.progress.xp.toLocaleString("vi-VN");
    $("#profile-name").textContent = state.profile.name || "Sĩ tử";
    $("#avatar-letter").textContent = (state.profile.name || "S").trim().charAt(0).toUpperCase();
    $("#rank-title").textContent = rankName();
    $("#rank-medal").textContent = rankGlyph();
    const overall = Math.round((completedLessonCount() / totalLessons) * 100);
    $("#overall-progress").style.width = `${overall}%`;
    $("#progress-caption").textContent = `${completedLessonCount()} / ${totalLessons} bài học hoàn thành`;
    $("#progress-percent").textContent = `${overall}%`;
    $("#stat-lessons").textContent = completedLessonCount();
    $("#stat-exams").textContent = `${passedCount()}/6`;
    $("#stat-streak").textContent = state.progress.daysStudied.length;
    $("#welcome-title").textContent = passedCount() === 6 ? "Trạng nguyên, triều đình đang đợi khanh!" : `${state.profile.name || "Sĩ tử"}, đường khoa cử đã mở!`;
    const activeStage = DATA.stages[currentStageIndex()];
    $("#welcome-subtitle").textContent = passedCount() === 6 ? "Hãy chọn một chức vị và bắt đầu hành trình làm quan." : `Thầy Mặc Bảo đang chờ con ở ${activeStage.hsk}.`;
    if (state.view === "home") renderHome();
    if (state.view === "map") renderMap();
    if (state.view === "study") renderStudy();
    if (state.view === "exam") renderExam();
    if (state.view === "records") renderRecords();
  }
  function markStudyDay(progress) {
    const today = new Date().toISOString().slice(0, 10);
    if (!progress.daysStudied.includes(today)) progress.daysStudied.push(today);
    progress.daysStudied = progress.daysStudied.slice(-90);
  }
  function navigate(view) {
    state.view = view;
    state.lesson = null;
    state.quiz = null;
    $$(".view").forEach((section) => section.classList.toggle("active", section.id === `view-${view}`));
    $$("[data-view]").forEach((button) => button.classList.toggle("active", button.dataset.view === view));
    const labels = { home: "ĐIỆN HỌC TẬP", map: "CON ĐƯỜNG KHOA CỬ", study: "BÀI HỌC HÁN NGỮ", exam: "TRƯỜNG THI", records: "BẢNG THÀNH TÍCH" };
    $("#crumb-current").textContent = labels[view] || "HÀNH TRÌNH";
    updateUserInterface();
    $("#app-content").focus({ preventScroll: true });
  }

  function renderHome() {
    const activeIndex = currentStageIndex();
    $("#stage-preview").innerHTML = DATA.stages.map((stage, index) => {
      const passed = state.progress.passedExams.includes(stage.id);
      const current = index === activeIndex;
      const locked = !stageUnlocked(index);
      const status = passed ? "ĐÃ ĐỖ" : current ? "ĐANG HỌC" : "CHƯA MỞ";
      return `<button class="stage-tile ${passed ? "" : current ? "current" : "locked"}" data-stage="${index}" ${locked ? "disabled" : ""}>
        <span class="stage-num">ĐỆ ${String(index + 1).padStart(2, "0")} KHOA</span><span class="stage-glyph">${stage.glyph}</span>
        <strong>${stage.hsk}</strong><small>${stage.title}</small><span class="stage-status">${status}</span>
      </button>`;
    }).join("");
  }
  function renderMap() {
    const activeIndex = currentStageIndex();
    $("#map-path").innerHTML = DATA.stages.map((stage, index) => {
      const passed = state.progress.passedExams.includes(stage.id);
      const unlocked = stageUnlocked(index);
      const ready = lessonsForStageDone(stage);
      let status = passed ? "ĐÃ VƯỢT KHOA THI" : unlocked ? ready ? "ĐỦ ĐIỀU KIỆN DỰ KHẢO" : "ĐANG ÔN TẬP" : "ĐANG NIÊM PHONG";
      let action = passed ? "Ôn lại bài" : ready ? "Vào trường thi" : "Vào học";
      return `<article class="map-node ${passed ? "passed" : index === activeIndex ? "current" : "locked"}">
        <div class="node-emblem">${stage.glyph}</div>
        <div class="node-copy"><div class="node-meta"><span>${stage.hsk}</span><span>·</span><span>${status}</span></div><h3>${stage.title}</h3><p>${stage.note} Danh vị sau khoa thi: ${stage.rank}.</p></div>
        <button class="node-button" data-map-action="${index}" ${unlocked ? "" : "disabled"}>${action}</button>
      </article>`;
    }).join("");
    const official = $("#official-panel");
    official.classList.toggle("unlocked", passedCount() === 6);
    if (passedCount() === 6) {
      const roles = [
        { id: "hàn lâm học sĩ", name: "Hàn Lâm học sĩ", desc: "Soạn chiếu thư, khảo cứu kinh sử và phụng mệnh trong Hàn Lâm viện." },
        { id: "tri phủ", name: "Tri phủ", desc: "Chăm lo việc học, dân sinh và công vụ tại một phủ." },
        { id: "ngự sử", name: "Ngự sử", desc: "Giữ phép công, xem xét chính sự và can gián điều chưa phải." }
      ];
      official.innerHTML = `<div class="eyebrow dark-eyebrow">金榜題名 · ĐÃ ĐỖ ĐIỆN THÍ</div><h3>Trạng nguyên đã đăng khoa — nay chọn đường quan lộ</h3><p>Danh vị cao nhất đã đạt. Hãy chọn một vai trò để mở chương tiếp theo của hành trình.</p><div class="official-choices">${roles.map((role) => `<button class="official-choice ${state.progress.appointment === role.id ? "selected" : ""}" data-role="${escapeHTML(role.id)}"><strong>${role.name}</strong><small>${role.desc}</small></button>`).join("")}</div>`;
    } else {
      official.innerHTML = "";
    }
    const world = state.progress.world || { x: 8, y: 58 };
    positionPlayer(world.x, world.y, false);
  }
  function renderStudy() {
    const unlocked = DATA.stages.slice(0, passedCount() + 1);
    const all = DATA.stages.map((stage, stageIndex) => {
      const canOpenStage = stageUnlocked(stageIndex);
      const cards = stage.lessons.map((lesson) => {
        const done = state.progress.completedLessons.includes(lesson.id);
        return `<button class="lesson-card ${done ? "done" : ""}" data-lesson="${lesson.id}" ${canOpenStage ? "" : "disabled"}>
          <span class="lesson-icon">${lesson.glyph}</span><span class="lesson-info"><small>${stage.hsk} · ${done ? "ĐÃ HOÀN THÀNH" : "BÀI HỌC"}</small><strong>${lesson.title}</strong><p>${lesson.subtitle} · ${lesson.words.length} từ mới</p></span><span class="lesson-arrow">${done ? "✓" : "→"}</span>${done ? "<span class=\"lesson-check\">✦</span>" : ""}
        </button>`;
      }).join("");
      return `<div class="study-section"><div class="study-section-heading"><div class="card-kicker">${stage.hsk} · ${stage.title}</div></div><div class="lesson-grid">${cards}</div></div>`;
    }).join("");
    $("#lesson-list").innerHTML = `${all}<div class="study-tip">Mẹo học: hãy đọc chữ Hán, nhẩm pinyin, nghe phát âm rồi chọn nghĩa tiếng Việt. Bài học phải hoàn thành để mở kỳ thi của cấp đó.</div>`;
  }
  function renderExam() {
    const index = currentStageIndex();
    const stage = DATA.stages[index];
    if (passedCount() === 6) {
      $("#exam-content").innerHTML = `<div class="exam-card"><div><div class="eyebrow dark-eyebrow">金榜題名</div><h2>Trạng nguyên cập đệ!</h2><p>Con đã vượt qua đủ sáu khoa thi HSK và đạt danh vị cao nhất của hành trình. Chuyển sang bản đồ quan lộ để nhận chức và mở chương mới.</p><div class="exam-actions"><button class="btn btn-gold" data-go="map">Vào triều nhận chức <span>→</span></button><button class="btn btn-ghost-light" data-go="records">Xem thành tích</button></div></div><div class="exam-seal">狀元</div></div>`;
      return;
    }
    const ready = lessonsForStageDone(stage);
    const attempts = state.progress.examAttempts[stage.id] || 0;
    const completedNames = stage.lessons.filter((lesson) => state.progress.completedLessons.includes(lesson.id)).length;
    const previous = passedCount() > 0 ? DATA.stages[passedCount() - 1].rank : "Học trò nhập môn";
    $("#exam-content").innerHTML = `<div class="exam-card"><div><div class="eyebrow dark-eyebrow">${stage.hsk} · ${stage.exam}</div><h2>${stage.title}</h2><p>${stage.note} Vượt qua kỳ khảo này để được phong danh vị <strong>${stage.rank}</strong>. Mỗi bài thi gồm 8 câu hỏi; cần đúng ít nhất 6 câu (75%) mới đỗ.</p><div class="exam-requirements"><span>${completedNames}/2 bài học hoàn thành</span><span>Chuẩn đỗ: 75%</span><span>Đã dự: ${attempts} lần</span></div><div class="exam-actions"><button class="btn btn-gold" id="start-exam" ${ready ? "" : "disabled"}>${attempts ? "Dự khảo lại" : "Vào trường thi"} <span>→</span></button><button class="btn btn-ghost-light" data-go="study">${ready ? "Ôn bài" : "Tiếp tục học"}</button></div></div><div class="exam-seal">${stage.glyph}</div></div>${ready ? "" : `<div class="exam-locked-note">Cổng trường thi hiện chưa mở. Con cần hoàn thành đủ hai bài học của ${stage.hsk}; hiện đã hoàn thành ${completedNames}/2 bài.</div>`}<div class="study-tip">Khoa thi đã vượt: ${state.progress.passedExams.length ? state.progress.passedExams.map((id) => DATA.stages.find((item) => item.id === id).hsk).join(" · ") : "Chưa có — hãy bắt đầu từ HSK 1."}</div>`;
  }
  function renderRecords() {
    const passed = state.progress.passedExams.map((id) => DATA.stages.find((stage) => stage.id === id));
    const recentDays = state.progress.daysStudied.slice().reverse().slice(0, 10);
    const lessonRows = DATA.stages.flatMap((stage) => stage.lessons.map((lesson) => ({ stage, lesson }))).filter((item) => state.progress.completedLessons.includes(item.lesson.id));
    $("#records-content").innerHTML = `<div class="records-grid">
      <section class="record-card"><div class="card-kicker">KHOA DANH ĐÃ ĐẠT</div><h3>${passedCount()} / 6 khoa thi</h3><div class="record-list">${passed.length ? passed.map((stage) => `<div class="record-row"><span>${stage.hsk} · ${stage.exam}</span><strong>${stage.rank}</strong></div>`).join("") : `<div class="empty-record">Chưa có khoa thi nào được ghi danh. Hãy bắt đầu học HSK 1.</div>`}</div></section>
      <section class="record-card"><div class="card-kicker">SỔ HỌC TẬP</div><h3>${completedLessonCount()} bài đã hoàn thành</h3><div class="record-list">${lessonRows.length ? lessonRows.map((item) => `<div class="record-row"><span>${item.stage.hsk} · ${item.lesson.title}</span><strong>Hoàn tất ✓</strong></div>`).join("") : `<div class="empty-record">Mỗi bài học hoàn thành sẽ được ghi vào đây.</div>`}</div></section>
      <section class="record-card"><div class="card-kicker">CHĂM HỌC MỖI NGÀY</div><h3>${state.progress.daysStudied.length} ngày có học</h3><div class="record-list">${recentDays.length ? recentDays.map((day) => `<div class="record-row"><span>${day}</span><strong>Đã tu học</strong></div>`).join("") : `<div class="empty-record">Ngày đầu tiên sẽ xuất hiện sau khi con bắt đầu bài học.</div>`}</div></section>
      <section class="record-card"><div class="card-kicker">HỒ SƠ SĨ TỬ</div><h3>${escapeHTML(state.profile.name)}</h3><div class="record-list"><div class="record-row"><span>Tên đăng nhập</span><strong>${escapeHTML(state.profile.username)}</strong></div><div class="record-row"><span>Danh vị</span><strong>${rankName()}</strong></div><div class="record-row"><span>Công danh</span><strong>${state.progress.xp}</strong></div><div class="record-row"><span>Quan chức</span><strong>${state.progress.appointment ? escapeHTML(state.progress.appointment) : "Chưa nhận chức"}</strong></div><div class="record-row"><span>Lưu trữ</span><strong>Trình duyệt hiện tại</strong></div></div></section>
    </div>`;
  }

  const pandaTeacher = `<svg class="panda-svg" viewBox="0 0 90 90" role="img" aria-label="Thầy giáo gấu trúc Mặc Bảo"><circle cx="25" cy="23" r="15" fill="#202d29"/><circle cx="65" cy="23" r="15" fill="#202d29"/><ellipse cx="45" cy="48" rx="34" ry="31" fill="#fffaf0" stroke="#d8c9a7" stroke-width="1.5"/><ellipse cx="33" cy="43" rx="8" ry="11" transform="rotate(24 33 43)" fill="#263532"/><ellipse cx="57" cy="43" rx="8" ry="11" transform="rotate(-24 57 43)" fill="#263532"/><circle cx="34" cy="43" r="2.5" fill="#fff"/><circle cx="56" cy="43" r="2.5" fill="#fff"/><ellipse cx="45" cy="56" rx="5" ry="3.6" fill="#263532"/><path d="M39 62 Q45 68 51 62" fill="none" stroke="#9e6653" stroke-width="2" stroke-linecap="round"/><path d="M17 16 Q45 -7 73 16 L67 21 Q45 5 23 21Z" fill="#244b3d" stroke="#d5b77e" stroke-width="2"/><path d="M45 6 V0" stroke="#d5b77e" stroke-width="3" stroke-linecap="round"/><path d="M23 73 Q45 66 67 73 L72 90 H18Z" fill="#3e735d"/><path d="M31 73 L45 86 L59 73" fill="#f5e9d0"/></svg>`;
  function addLearningQuestion() {
    const word = state.lesson.words[state.lessonIndex];
    const choices = shuffleChoices(word);
    state.lessonCurrentChoices = choices;
    state.lessonAnswered = false;
    $("#lesson-step-label").textContent = `${String(state.lessonIndex + 1).padStart(2, "0")} / ${String(state.lesson.words.length).padStart(2, "0")}`;
    $("#lesson-level-label").textContent = `${state.lessonStage.hsk} · ${state.lesson.title.toUpperCase()}`;
    const percent = Math.round((state.lessonIndex / state.lesson.words.length) * 100);
    $("#lesson-content").innerHTML = `<div class="lesson-shell">
      <div class="lesson-progress-head"><span>TIẾN ĐỘ BÀI HỌC</span><strong>${percent}%</strong></div><div class="lesson-progress"><span style="width:${percent}%"></span></div>
      <div class="panda-teacher">${pandaTeacher}<div class="teacher-speech"><strong>Thầy Mặc Bảo</strong><p>Con hãy đọc chữ Hán, nhớ âm đọc và chọn nghĩa tiếng Việt đúng nhé.</p></div></div>
      <div class="question-kicker">TỪ VỰNG · CHỌN NGHĨA CHÍNH XÁC</div><h2 class="lesson-question">Từ này có nghĩa là gì?</h2><p class="lesson-hint">Chọn một đáp án đúng trong bốn lựa chọn bên dưới.</p>
      <div class="word-display"><div class="word-hanzi">${word.hanzi}</div><div class="word-pinyin">${word.pinyin}</div><button class="guest-button" id="speak-word" type="button">▶ Nghe phát âm</button></div>
      <div class="answer-grid">${choices.map((choice, index) => `<button class="answer-option" data-answer="${index}">${String.fromCharCode(65 + index)}. ${escapeHTML(choice.meaning)}</button>`).join("")}</div>
      <div class="lesson-feedback" id="lesson-feedback" aria-live="polite">Thầy tin con sẽ làm được. Hãy suy nghĩ kỹ trước khi chọn.</div>
      <div class="lesson-bottom"><small>Chọn đúng để mở lời giải và sang từ tiếp theo.</small><button class="btn btn-gold" id="lesson-next" disabled>Tiếp tục <span>→</span></button></div>
    </div>`;
  }
  function startLesson(lessonId) {
    const found = lessonById(lessonId);
    if (!found) return;
    const stageIndex = DATA.stages.findIndex((stage) => stage.lessons.some((lesson) => lesson.id === lessonId));
    if (!stageUnlocked(stageIndex)) return showToast("Cần vượt khoa thi trước để mở cấp học tiếp theo.");
    state.lesson = found;
    state.lessonStage = DATA.stages[stageIndex];
    state.lessonIndex = 0;
    state.view = "lesson";
    $$(".view").forEach((section) => section.classList.toggle("active", section.id === "view-lesson"));
    addLearningQuestion();
  }
  function answerLesson(choiceIndex) {
    if (state.lessonAnswered) return;
    const word = state.lesson.words[state.lessonIndex];
    const selected = state.lessonCurrentChoices[choiceIndex];
    const buttons = $$(".answer-option", $("#lesson-content"));
    const feedback = $("#lesson-feedback");
    if (selected.meaning === word.meaning) {
      state.lessonAnswered = true;
      buttons.forEach((button, index) => {
        button.disabled = true;
        if (state.lessonCurrentChoices[index].meaning === word.meaning) button.classList.add("correct");
      });
      feedback.className = "lesson-feedback success";
      feedback.innerHTML = `Chính xác! <strong>${word.hanzi} (${word.pinyin})</strong> nghĩa là ${escapeHTML(word.meaning)}.<br><span>${escapeHTML(word.example)} — ${escapeHTML(word.translation)}</span>`;
      $("#lesson-next").disabled = false;
      $("#lesson-next").innerHTML = state.lessonIndex === state.lesson.words.length - 1 ? "Hoàn thành bài <span>✦</span>" : "Từ tiếp theo <span>→</span>";
    } else {
      buttons[choiceIndex].classList.add("wrong");
      feedback.className = "lesson-feedback error";
      feedback.textContent = "Chưa chính xác. Thầy gợi ý con đọc lại pinyin và thử chọn lần nữa nhé.";
      window.setTimeout(() => buttons[choiceIndex] && buttons[choiceIndex].classList.remove("wrong"), 600);
    }
  }
  function nextLessonQuestion() {
    if (!state.lessonAnswered) return;
    if (state.lessonIndex < state.lesson.words.length - 1) {
      state.lessonIndex += 1;
      addLearningQuestion();
      return;
    }
    const firstCompletion = !state.progress.completedLessons.includes(state.lesson.id);
    NQVAuth.updateProgress((progress) => {
      if (firstCompletion) {
        progress.completedLessons.push(state.lesson.id);
        progress.xp += 20;
      }
      markStudyDay(progress);
    });
    state.progress = state.profile.progress;
    showToast(firstCompletion ? "Đã hoàn thành bài học! +20 công danh." : "Ôn tập hoàn tất. Tiến trình đã được lưu.");
    navigate("study");
  }
  function speakCurrentWord() {
    if (!state.lesson || !window.speechSynthesis) return showToast("Trình duyệt này chưa hỗ trợ đọc văn bản.");
    const word = state.lesson.words[state.lessonIndex];
    const utterance = new SpeechSynthesisUtterance(word.hanzi);
    utterance.lang = "zh-CN";
    utterance.rate = 0.82;
    speechSynthesis.cancel();
    speechSynthesis.speak(utterance);
  }
  function startExam() {
    const stage = DATA.stages[currentStageIndex()];
    if (!lessonsForStageDone(stage)) return showToast("Hãy hoàn thành đủ bài học trước khi dự khảo.");
    const words = shuffle(stage.lessons.flatMap((lesson) => lesson.words));
    state.quiz = { stage, questions: words.map((word) => ({ word, choices: shuffleChoices(word) })) };
    state.quizIndex = 0;
    state.quizAnswers = [];
    state.view = "quiz";
    NQVAuth.updateProgress((progress) => {
      progress.examAttempts[stage.id] = (progress.examAttempts[stage.id] || 0) + 1;
      markStudyDay(progress);
    });
    state.progress = state.profile.progress;
    $$(".view").forEach((section) => section.classList.toggle("active", section.id === "view-quiz"));
    renderQuizQuestion();
  }
  function renderQuizQuestion() {
    const question = state.quiz.questions[state.quizIndex];
    const stage = state.quiz.stage;
    $("#quiz-label").textContent = `${stage.hsk} · ${stage.exam.toUpperCase()}`;
    $("#quiz-step").textContent = `${String(state.quizIndex + 1).padStart(2, "0")} / 08`;
    $("#quiz-content").innerHTML = `<div class="quiz-shell"><div class="quiz-head"><div><small>ĐIỆN THÍ · CÂU HỎI ${state.quizIndex + 1}</small><h2>${stage.title}</h2></div><span class="quiz-timer">ĐẠT CHUẨN · 6/8 CÂU</span></div>
      <div class="lesson-progress"><span style="width:${((state.quizIndex) / 8) * 100}%"></span></div>
      <div class="quiz-question"><small>尋字釋義 · GIẢI NGHĨA TỪ NGỮ</small><div class="word-hanzi">${question.word.hanzi}</div><div class="word-pinyin">${question.word.pinyin}</div><p>Hãy chọn nghĩa tiếng Việt phù hợp nhất.</p></div>
      <div class="answer-grid">${question.choices.map((choice, index) => `<button class="answer-option" data-quiz-answer="${index}">${String.fromCharCode(65 + index)}. ${escapeHTML(choice.meaning)}</button>`).join("")}</div>
      <div class="lesson-feedback" id="quiz-feedback" aria-live="polite">Bình tĩnh suy xét, chọn đáp án con cho là chính xác nhất.</div>
      <div class="quiz-actions"><small>Đã trả lời ${state.quizAnswers.length}/8 câu</small><button class="btn btn-gold" id="quiz-next" disabled>${state.quizIndex === 7 ? "Nộp quyển thi" : "Câu tiếp theo"} <span>→</span></button></div>
    </div>`;
  }
  function selectQuizAnswer(index) {
    if (state.quizAnswers[state.quizIndex] !== undefined) return;
    const question = state.quiz.questions[state.quizIndex];
    state.quizAnswers[state.quizIndex] = question.choices[index].meaning === question.word.meaning;
    const correct = state.quizAnswers[state.quizIndex];
    const buttons = $$(".answer-option", $("#quiz-content"));
    buttons.forEach((button, buttonIndex) => {
      button.disabled = true;
      if (question.choices[buttonIndex].meaning === question.word.meaning) button.classList.add("correct");
      if (buttonIndex === index && !correct) button.classList.add("wrong");
    });
    $("#quiz-feedback").className = `lesson-feedback ${correct ? "success" : "error"}`;
    $("#quiz-feedback").textContent = correct ? "Đúng. Con đã ghi thêm một điểm trong quyển thi." : `Chưa đúng. Đáp án là “${question.word.meaning}”. Hãy ghi nhớ cho lần sau.`;
    $("#quiz-next").disabled = false;
  }
  function finishExam() {
    const stage = state.quiz.stage;
    const score = state.quizAnswers.filter(Boolean).length;
    const passed = score >= 6;
    if (passed && !state.progress.passedExams.includes(stage.id)) {
      NQVAuth.updateProgress((progress) => {
        progress.passedExams.push(stage.id);
        progress.passedExams.sort((a, b) => a - b);
        progress.xp += 60 + score * 5;
        markStudyDay(progress);
      });
    } else {
      NQVAuth.updateProgress((progress) => { progress.xp += score * 2; markStudyDay(progress); });
    }
    state.progress = state.profile.progress;
    $("#quiz-step").textContent = "KẾT QUẢ";
    const nextRank = stage.rank;
    const message = passed ? (stage.id === 6 ? "Bệ hạ ngự điện, con đã đỗ Trạng nguyên!" : `Con đã vượt khoa thi và được phong danh vị ${nextRank}.`) : "Khoa thi chưa đạt điểm chuẩn. Hãy ôn tập rồi quay lại trường thi.";
    $("#quiz-content").innerHTML = `<div class="quiz-shell"><div class="quiz-score"><div class="score-seal">${score}/8</div><div class="eyebrow dark-eyebrow">${passed ? "金榜題名 · ĐỀ DANH BẢNG VÀNG" : "再接再厲 · GẮNG SỨC LẦN SAU"}</div><h2>${passed ? (stage.id === 6 ? "Trạng nguyên cập đệ!" : "Vượt khoa thành danh!") : "Chưa qua kỳ khảo"}</h2><p>${message} Kết quả: ${score}/8 câu đúng (${Math.round(score / 8 * 100)}%). ${passed ? "Cổng tiếp theo đã mở và tiến trình đã được lưu." : "Cần đạt ít nhất 6/8 câu để được tiến bậc."}</p><div class="score-actions">${passed ? `<button class="btn btn-gold" data-go="map">Xem khoa lộ mới <span>→</span></button>` : `<button class="btn btn-gold" id="retake-exam">Ôn lại và thi lại <span>↻</span></button>`}<button class="btn btn-ghost-light" data-go="records">Xem thành tích</button></div></div></div>`;
    updateUserInterface();
  }
  function advanceQuiz() {
    if (state.quizAnswers[state.quizIndex] === undefined) return;
    if (state.quizIndex < 7) {
      state.quizIndex += 1;
      renderQuizQuestion();
    } else finishExam();
  }

  function positionPlayer(x, y, save = true) {
    const boundedX = Math.max(5, Math.min(95, x));
    const boundedY = Math.max(25, Math.min(76, y));
    const player = $("#walk-panda");
    if (!player) return;
    player.style.left = `${boundedX}%`;
    player.style.top = `${boundedY}%`;
    $("#world-coord").textContent = `${String(Math.round(boundedX)).padStart(2, "0")} · ${String(Math.round(boundedY)).padStart(2, "0")}`;
    if (save && state.progress) NQVAuth.updateProgress((progress) => { progress.world = { x: boundedX, y: boundedY }; });
    const message = $("#world-message");
    if (message) {
      if (boundedX < 32 && boundedY > 44) message.textContent = "Con đã đến gần Tàng Thư Các. Chạm vào tòa nhà để vào lớp học.";
      else if (boundedX > 67 && boundedY > 43) message.textContent = "Con đã đến Trường Thi. Chạm vào tòa điện để xem điều kiện dự khảo.";
      else message.textContent = "Sĩ tử có thể đi dạo quanh thư viện. Dùng phím mũi tên / WASD hoặc nút điều hướng.";
    }
  }
  function movePlayer(direction) {
    if (state.view !== "map") return;
    const world = state.progress.world || { x: 8, y: 58 };
    const amount = 5;
    let x = world.x;
    let y = world.y;
    if (direction === "left") x -= amount;
    if (direction === "right") x += amount;
    if (direction === "up") y -= amount;
    if (direction === "down") y += amount;
    positionPlayer(x, y);
  }
  function openAuth(mode = "register") {
    setAuthMode(mode);
    $("#auth-modal").hidden = false;
    $("#auth-error").textContent = "";
    $("#auth-username").focus();
  }
  function closeAuth() { $("#auth-modal").hidden = true; }
  function setAuthMode(mode) {
    state.authMode = mode;
    $$("[data-auth-mode]").forEach((button) => button.classList.toggle("selected", button.dataset.authMode === mode));
    $("#auth-title").textContent = mode === "register" ? "Ghi danh nhập học" : "Trở lại trường xưa";
    $("#name-field").hidden = mode !== "register";
    $("#auth-name").required = mode === "register";
    $("#auth-password").autocomplete = mode === "register" ? "new-password" : "current-password";
    $("#auth-submit").innerHTML = mode === "register" ? "Đăng ký nhập học <span>→</span>" : "Đăng nhập <span>→</span>";
    $("#auth-error").textContent = "";
  }
  async function submitAuth(event) {
    event.preventDefault();
    const username = $("#auth-username").value;
    const password = $("#auth-password").value;
    const displayName = $("#auth-name").value;
    const remember = $("#auth-remember").checked;
    const submit = $("#auth-submit");
    submit.disabled = true;
    submit.textContent = "Đang ghi danh…";
    try {
      const profile = state.authMode === "register"
        ? await NQVAuth.register(username, password, displayName, remember)
        : await NQVAuth.login(username, password, remember);
      state.profile = profile;
      state.progress = profile.progress;
      closeAuth();
      updateUserInterface();
      showToast(state.authMode === "register" ? "Đã ghi danh thành công. Hồ sơ đã được lưu." : "Đăng nhập thành công. Đã tải tiến trình tài khoản.");
    } catch (error) {
      $("#auth-error").textContent = error.message || "Không thể hoàn tất thao tác.";
    } finally {
      submit.disabled = false;
      submit.innerHTML = state.authMode === "register" ? "Đăng ký nhập học <span>→</span>" : "Đăng nhập <span>→</span>";
    }
  }

  document.addEventListener("click", (event) => {
    if (event.target.closest("#lesson-next")) { nextLessonQuestion(); return; }
    if (event.target.closest("#quiz-next")) { advanceQuiz(); return; }
    const nav = event.target.closest("[data-view]");
    if (nav) { navigate(nav.dataset.view); return; }
    const go = event.target.closest("[data-go]");
    if (go) { navigate(go.dataset.go); return; }
    const lesson = event.target.closest("[data-lesson]");
    if (lesson) { startLesson(lesson.dataset.lesson); return; }
    const answer = event.target.closest("[data-answer]");
    if (answer) { answerLesson(Number(answer.dataset.answer)); return; }
    const quizAnswer = event.target.closest("[data-quiz-answer]");
    if (quizAnswer) { selectQuizAnswer(Number(quizAnswer.dataset.quizAnswer)); return; }
    const stage = event.target.closest("[data-stage]");
    if (stage) { navigate("map"); return; }
    const mapAction = event.target.closest("[data-map-action]");
    if (mapAction) {
      const index = Number(mapAction.dataset.mapAction);
      const target = DATA.stages[index];
      if (lessonsForStageDone(target) && !state.progress.passedExams.includes(target.id)) navigate("exam");
      else navigate("study");
      return;
    }
    const role = event.target.closest("[data-role]");
    if (role) {
      NQVAuth.updateProgress((progress) => { progress.appointment = role.dataset.role; });
      state.progress = state.profile.progress;
      renderMap();
      showToast(`Đã nhận chức ${role.dataset.role}. Con đường quan lộ bắt đầu!`);
      return;
    }
    const move = event.target.closest("[data-move]");
    if (move) { movePlayer(move.dataset.move); return; }
    const option = event.target.closest(".scene-building");
    if (option) { navigate(option.classList.contains("building-library") ? "study" : "exam"); return; }
  });
  document.addEventListener("keydown", (event) => {
    if (state.view !== "map" || !$("#auth-modal").hidden) return;
    const keyMap = { ArrowUp: "up", w: "up", W: "up", ArrowDown: "down", s: "down", S: "down", ArrowLeft: "left", a: "left", A: "left", ArrowRight: "right", d: "right", D: "right" };
    if (keyMap[event.key]) { event.preventDefault(); movePlayer(keyMap[event.key]); }
  });
  $("#continue-button").addEventListener("click", () => navigate("study"));
  $("#profile-button").addEventListener("click", () => openAuth("login"));
  $("#lesson-back").addEventListener("click", () => navigate("study"));
  $("#quiz-exit").addEventListener("click", () => navigate("exam"));
  $("#auth-close").addEventListener("click", closeAuth);
  $("#auth-modal").addEventListener("click", (event) => { if (event.target === $("#auth-modal")) closeAuth(); });
  $("#auth-form").addEventListener("submit", submitAuth);
  $("#guest-button").addEventListener("click", () => {
    const profile = NQVAuth.continueAsGuest();
    state.profile = profile;
    state.progress = profile.progress;
    closeAuth();
    updateUserInterface();
    showToast("Đã vào học thử. Tiến trình vẫn được lưu trên thiết bị này.");
  });
  $$("[data-auth-mode]").forEach((button) => button.addEventListener("click", () => setAuthMode(button.dataset.authMode)));
  $("#auth-password").addEventListener("input", () => { $("#auth-error").textContent = ""; });
  document.addEventListener("click", (event) => {
    if (event.target.closest("#speak-word")) speakCurrentWord();
    if (event.target.closest("#start-exam")) startExam();
    if (event.target.closest("#retake-exam")) startExam();
  });
  window.addEventListener("nqv-progress-changed", (event) => {
    if (state.profile) state.progress = state.profile.progress;
    if (state.view === "map") renderMap();
  });
  window.addEventListener("nqv-profile-changed", (event) => {
    if (event.detail) {
      state.profile = event.detail;
      state.progress = event.detail.progress;
    }
  });

  async function boot() {
    const profile = await NQVAuth.init();
    if (!profile) {
      state.profile = NQVAuth.continueAsGuest();
    } else {
      state.profile = profile;
    }
    state.progress = state.profile.progress;
    updateUserInterface();
    if ("serviceWorker" in navigator && location.protocol.startsWith("http")) {
      navigator.serviceWorker.register("./sw.js").catch((error) => console.warn("Không đăng ký được service worker:", error));
    }
  }
  boot().catch((error) => {
    console.error(error);
    showToast("Không thể khởi tạo hồ sơ. Vui lòng tải lại trang.");
  });
})();

