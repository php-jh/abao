const scenarios = [
  {
    id: "public-speaking",
    title: "公开演讲",
    opening: "What is your greatest weakness? Please answer with a problem, an action, and a positive result.",
    zh: "你在面试中被问到最大的弱点。请结合自己的专业和未来工作岗位，选择一个真实且恰当的弱点，用 PAR 模型回答。",
    target: "I used to struggle with public speaking. To improve this, I joined the school's public speaking club. As a result, I became more confident and my last speech was great!",
    next: "Good. Can you give one work-related example of how this improvement will help you in the future?",
    hints: ["I used to struggle with...", "To improve this...", "As a result..."]
  },
  {
    id: "time-management",
    title: "时间管理",
    opening: "What is your greatest weakness? Try to tell a short story about how you overcame it.",
    zh: "请围绕时间管理回答：先说问题，再说改进方法，最后说积极结果。",
    target: "I used to struggle with time management. To improve this, I started to make a to-do list every day. As a result, I can now finish my tasks on time and feel less stressed.",
    next: "Nice answer. How will this habit help you with future work responsibilities?",
    hints: ["time management", "make a to-do list", "finish my tasks on time"]
  },
  {
    id: "team-discussion",
    title: "团队讨论",
    opening: "What is your greatest weakness in teamwork or communication? Use the PAR model to answer.",
    zh: "请围绕团队讨论回答：说出不足、改进行动和积极结果。",
    target: "I used to struggle with team discussions. To improve this, I started using AI talk partners to practice team discussions. As a result, I can share my suggestions in team discussions.",
    next: "Great. What suggestions would you share in a real team discussion?",
    hints: ["team discussions", "AI talk partners", "share my suggestions"]
  }
];

const warmupWords = [
  ["weakness", "弱点，不足"],
  ["interview", "面试"],
  ["identify problem", "找出问题"],
  ["overcome", "克服"],
  ["positive result", "积极结果"],
  ["work-related", "与工作相关的"],
  ["skill", "技能"],
  ["responsibility", "职责"],
  ["position", "岗位"],
  ["learn and grow", "学习与成长"],
  ["give examples", "举例"],
  ["tell a story", "讲故事"],
  ["time management", "时间管理"],
  ["public speaking", "公开演讲"],
  ["team discussion", "团队讨论"],
  ["a to-do list", "任务清单"],
  ["AI talk partner", "AI 对话伙伴"],
  ["stress", "压力"],
  ["presentation", "演讲"],
  ["confident", "自信"]
];

const metrics = ["语句词汇搭配", "用词准确度", "句子结构", "发音", "语速", "流利度", "完整度"];
const state = {
  current: scenarios[0],
  startedAt: 0,
  recognition: null,
  recognizing: false,
  lastScore: null,
  avatarStage: "warmup",
  currentAudio: null,
  finalTranscript: "",
  interimTranscript: "",
  audioStream: null,
  audioContext: null,
  audioProcessor: null,
  audioSource: null,
  audioChunks: [],
  audioSampleRate: 44100
};

const $ = (selector) => document.querySelector(selector);
const dialogue = $("#dialogue");
const input = $("#studentInput");

function init() {
  renderScenarioOptions();
  renderWarmup();
  bindEvents();
  selectScenario(scenarios[0].id);
  setupSpeechRecognition();
}

function renderScenarioOptions() {
  $("#scenarioSelect").innerHTML = scenarios
    .map((item) => `<option value="${item.id}">${item.title}</option>`)
    .join("");
}

function renderWarmup() {
  $("#wordGrid").innerHTML = warmupWords
    .map(([word, zh]) => `
      <article class="word-card">
        <strong>${word}</strong>
        <p>${zh}</p>
        <button class="ghost-button" type="button" data-speak="${escapeHtml(word)}">跟读</button>
      </article>
    `)
    .join("");
}

function bindEvents() {
  $("#enterButton").addEventListener("click", enterSystem);
  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => switchTab(tab.dataset.tab));
  });
  $("#scenarioSelect").addEventListener("change", (event) => selectScenario(event.target.value));
  $("#recordButton").addEventListener("click", toggleRecording);
  $("#finishButton").addEventListener("click", finishReading);
  $("#speakPromptButton").addEventListener("click", () => speak(state.current.opening));
  $("#hintButton").addEventListener("click", showHint);
  $("#translateButton").addEventListener("click", showTranslation);
  document.body.addEventListener("click", (event) => {
    const text = event.target.closest("[data-speak]")?.dataset.speak;
    if (text) speak(text);
  });
}

function enterSystem() {
  $("#splashScreen").hidden = true;
  $("#appShell").hidden = false;
  switchTab("warmup");
}

function switchTab(tabName) {
  document.querySelectorAll(".tab").forEach((tab) => tab.classList.toggle("active", tab.dataset.tab === tabName));
  ["warmup", "practice", "challenge"].forEach((id) => {
    document.getElementById(id).hidden = id !== tabName;
  });
  $("#appShell").dataset.stage = tabName;
  state.avatarStage = tabName;
  $("#pandaAvatarImage").src = tabName === "challenge" ? "./assets/panda-suit.png" : "./assets/panda-hanfu.png";
}

function selectScenario(id) {
  state.current = scenarios.find((item) => item.id === id) || scenarios[0];
  $("#scenarioSelect").value = state.current.id;
  $("#scenarioTitle").textContent = state.current.title;
  $("#agentHint").textContent = `当前情景：${state.current.title}`;
  input.value = "";
  input.placeholder = "点击开始录入，或直接输入一句英文用于演示。";
  $("#feedbackPanel").hidden = true;
  renderDialogue([{ role: "ai", text: state.current.opening, sub: state.current.zh }]);
}

function renderDialogue(messages) {
  dialogue.innerHTML = messages
    .map((msg) => `<div class="message ${msg.role}">${escapeHtml(msg.text)}${msg.sub ? `<small>${escapeHtml(msg.sub)}</small>` : ""}</div>`)
    .join("");
  dialogue.scrollTop = dialogue.scrollHeight;
}

function appendMessage(role, text, sub = "") {
  const item = document.createElement("div");
  item.className = `message ${role}`;
  item.innerHTML = `${escapeHtml(text)}${sub ? `<small>${escapeHtml(sub)}</small>` : ""}`;
  dialogue.appendChild(item);
  dialogue.scrollTop = dialogue.scrollHeight;
}

function setupSpeechRecognition() {
  if (!navigator.mediaDevices?.getUserMedia) {
    $("#recordButton").textContent = "🎙 浏览器不支持";
    $("#recordButton").title = "当前浏览器不支持录音，请使用 Chrome 或 Edge。";
    input.placeholder = "当前浏览器不支持录音，请使用 Chrome 或 Edge，或直接输入英文。";
  }
}

async function toggleRecording() {
  if (window.AliyunNlsAndroid) {
    toggleAndroidRecording();
    return;
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    input.focus();
    return;
  }
  if (state.recognizing) {
    await stopRecordingAndRecognize();
    return;
  }
  await startRecording();
}

function toggleAndroidRecording() {
  if (state.recognizing) {
    state.recognizing = false;
    $("#recordButton").classList.remove("recording");
    $("#recordButton").textContent = "识别中...";
    $("#recordButton").disabled = true;
    input.placeholder = "安卓端正在识别...";
    window.AliyunNlsAndroid.stopRecording();
    return;
  }
  input.value = "";
  state.startedAt = Date.now();
  state.recognizing = true;
  $("#recordButton").classList.add("recording");
  $("#recordButton").textContent = "■ 停止并识别";
  input.placeholder = "安卓端正在录音，请开始说英文...";
  window.AliyunNlsAndroid.startRecording();
}

async function startRecording() {
  input.value = "";
  state.finalTranscript = "";
  state.interimTranscript = "";
  state.audioChunks = [];
  state.startedAt = Date.now();
  input.placeholder = "正在录音，请开始说英文。再次点击按钮后识别文本...";

  state.audioStream = await navigator.mediaDevices.getUserMedia({
    audio: {
      channelCount: 1,
      echoCancellation: true,
      noiseSuppression: true,
      autoGainControl: true
    }
  });
  state.audioContext = new (window.AudioContext || window.webkitAudioContext)();
  state.audioSampleRate = state.audioContext.sampleRate;
  state.audioSource = state.audioContext.createMediaStreamSource(state.audioStream);
  state.audioProcessor = state.audioContext.createScriptProcessor(4096, 1, 1);
  state.audioProcessor.onaudioprocess = (event) => {
    state.audioChunks.push(new Float32Array(event.inputBuffer.getChannelData(0)));
  };
  state.audioSource.connect(state.audioProcessor);
  state.audioProcessor.connect(state.audioContext.destination);
  state.recognizing = true;
  $("#recordButton").classList.add("recording");
  $("#recordButton").textContent = "■ 停止并识别";
}

async function stopRecordingAndRecognize() {
  state.recognizing = false;
  $("#recordButton").classList.remove("recording");
  $("#recordButton").textContent = "识别中...";
  $("#recordButton").disabled = true;
  input.placeholder = "正在上传阿里云识别...";

  if (state.audioProcessor) state.audioProcessor.disconnect();
  if (state.audioSource) state.audioSource.disconnect();
  if (state.audioStream) state.audioStream.getTracks().forEach((track) => track.stop());
  if (state.audioContext) await state.audioContext.close();

  try {
    const wav = encodeWav(mergeAudioChunks(state.audioChunks), state.audioSampleRate, 16000);
    const response = await fetch("/api/asr", {
      method: "POST",
      headers: { "Content-Type": "audio/wav" },
      body: wav
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || "语音识别失败");
    input.value = data.text || "";
    input.placeholder = data.text ? "识别完成，可继续修改或点击完成朗读。" : "未识别到内容，请重试或直接输入英文。";
  } catch (error) {
    input.placeholder = `${error.message}。也可以直接输入英文用于演示。`;
  } finally {
    $("#recordButton").disabled = false;
    $("#recordButton").textContent = "🎙 开始录入";
    state.audioStream = null;
    state.audioContext = null;
    state.audioProcessor = null;
    state.audioSource = null;
    state.audioChunks = [];
  }
}

function finishReading() {
  if (state.recognizing) {
    stopRecordingAndRecognize();
    return;
  }
  const answer = input.value.trim();
  if (!answer) {
    input.value = state.current.target;
  }
  const finalAnswer = input.value.trim();
  appendMessage("student", finalAnswer);
  appendMessage("ai", state.current.next, "阿宝会根据你的回答继续追问。");
  const elapsed = Math.max(5, (Date.now() - (state.startedAt || Date.now() - 12000)) / 1000);
  state.lastScore = scoreAnswer(finalAnswer, state.current.target, elapsed);
  renderFeedback(state.lastScore);
  speak(state.current.next);
}

function mergeAudioChunks(chunks) {
  const length = chunks.reduce((sum, chunk) => sum + chunk.length, 0);
  const result = new Float32Array(length);
  let offset = 0;
  chunks.forEach((chunk) => {
    result.set(chunk, offset);
    offset += chunk.length;
  });
  return result;
}

function encodeWav(samples, sourceRate, targetRate) {
  const pcm = downsample(samples, sourceRate, targetRate);
  const buffer = new ArrayBuffer(44 + pcm.length * 2);
  const view = new DataView(buffer);
  writeString(view, 0, "RIFF");
  view.setUint32(4, 36 + pcm.length * 2, true);
  writeString(view, 8, "WAVE");
  writeString(view, 12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, targetRate, true);
  view.setUint32(28, targetRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeString(view, 36, "data");
  view.setUint32(40, pcm.length * 2, true);
  let offset = 44;
  pcm.forEach((sample) => {
    const clipped = Math.max(-1, Math.min(1, sample));
    view.setInt16(offset, clipped < 0 ? clipped * 0x8000 : clipped * 0x7fff, true);
    offset += 2;
  });
  return new Blob([view], { type: "audio/wav" });
}

function downsample(samples, sourceRate, targetRate) {
  if (sourceRate === targetRate) return samples;
  const ratio = sourceRate / targetRate;
  const length = Math.round(samples.length / ratio);
  const result = new Float32Array(length);
  for (let i = 0; i < length; i += 1) {
    const start = Math.floor(i * ratio);
    const end = Math.floor((i + 1) * ratio);
    let sum = 0;
    let count = 0;
    for (let j = start; j < end && j < samples.length; j += 1) {
      sum += samples[j];
      count += 1;
    }
    result[i] = count ? sum / count : 0;
  }
  return result;
}

function writeString(view, offset, text) {
  for (let i = 0; i < text.length; i += 1) {
    view.setUint8(offset + i, text.charCodeAt(i));
  }
}

function scoreAnswer(answer, target, elapsedSeconds) {
  const answerWords = normalize(answer).split(" ").filter(Boolean);
  const targetWords = normalize(target).split(" ").filter(Boolean);
  const overlap = answerWords.filter((word) => targetWords.includes(word)).length;
  const coverage = targetWords.length ? overlap / targetWords.length : 0;
  const lengthRatio = Math.min(1, answerWords.length / Math.max(1, targetWords.length));
  const hasVerb = /\b(am|is|are|was|were|be|used|struggle|improve|joined|became|started|make|finish|feel|share|practice|help|learn|grow|overcome)\b/i.test(answer);
  const hasConnector = /\b(and|because|with|for|to|as|if|when)\b/i.test(answer);
  const wpm = Math.round((answerWords.length / elapsedSeconds) * 60);
  const speedScore = clamp(100 - Math.abs(wpm - 115) * 0.8, 55, 100);

  const values = {
    "语句词汇搭配": Math.round(clamp(coverage * 84 + (hasConnector ? 12 : 0), 45, 100)),
    "用词准确度": Math.round(clamp(coverage * 90 + 8, 48, 100)),
    "句子结构": Math.round(clamp((hasVerb ? 64 : 38) + (hasConnector ? 22 : 8) + lengthRatio * 14, 42, 100)),
    "发音": Math.round(clamp(coverage * 74 + speedScore * 0.22, 50, 100)),
    "语速": Math.round(speedScore),
    "流利度": Math.round(clamp(lengthRatio * 68 + speedScore * 0.26, 48, 100)),
    "完整度": Math.round(clamp(lengthRatio * 92 + 6, 40, 100))
  };
  const total = Math.round(Object.values(values).reduce((sum, item) => sum + item, 0) / metrics.length);
  return { values, total, wpm, coverage };
}

function renderFeedback(result) {
  $("#feedbackPanel").hidden = false;
  $("#totalScore").textContent = result.total;
  $("#speedValue").textContent = result.wpm;
  $("#speedNeedle").style.transform = `rotate(${clamp((result.wpm / 220) * 130 - 65, -65, 65)}deg)`;
  $("#metricList").innerHTML = metrics
    .map((name) => `
      <div class="metric">
        <span>${name}</span>
        <div class="bar"><i style="--value:${result.values[name]}%"></i></div>
        <strong>${result.values[name]}</strong>
      </div>
    `)
    .join("");
  $("#analysisText").textContent = buildAnalysis(result);
  $("#correctionText").innerHTML = `<strong>参考优化：</strong>${escapeHtml(suggestCorrection(input.value, state.current.target))}`;
}

function buildAnalysis(result) {
  const weak = Object.entries(result.values).sort((a, b) => a[1] - b[1]).slice(0, 2).map(([name]) => name);
  return `本次回答综合分 ${result.total}。主要优势是能覆盖情景关键词；建议重点提升 ${weak.join("、")}。回答时可补充问题、行动和积极结果，让表达更完整。`;
}

function suggestCorrection(answer, target) {
  const normalized = normalize(answer);
  if (!/\b(am|is|are|would|need|work|join|look|make|share|ask|used|struggle|improve|became|started|finish|feel|practice)\b/i.test(answer)) {
    return `请补充清晰谓语结构。可参考：${target}`;
  }
  if (!normalized.includes("as a result")) {
    return `建议补充结果句。可参考：${target}`;
  }
  if (answer.split(/\s+/).length < 7) {
    return `回答偏短，可以扩展为：${target}`;
  }
  return target;
}

function showHint() {
  appendMessage("ai", `You can use: ${state.current.hints.join(" / ")}`, "这些短语可以直接放进回答里。");
}

function showTranslation() {
  appendMessage("ai", state.current.zh, "中文提示");
}

function speak(text) {
  if (!text) return;
  if ("speechSynthesis" in window && "SpeechSynthesisUtterance" in window) {
    speakWithBrowserVoice(text);
    return;
  }
  playLocalAudio(text);
}

function speakWithBrowserVoice(text) {
  if (!text || !("speechSynthesis" in window) || !("SpeechSynthesisUtterance" in window)) return;
  const play = () => {
    const voices = window.speechSynthesis.getVoices();
    const voice = voices.find((item) => item.lang === "en-US") || voices.find((item) => item.lang?.startsWith("en"));
    window.speechSynthesis.cancel();
    window.speechSynthesis.resume();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "en-US";
    utterance.rate = 0.9;
    utterance.pitch = 1;
    if (voice) utterance.voice = voice;
    utterance.onerror = () => playLocalAudio(text);
    window.speechSynthesis.speak(utterance);
  };

  if (window.speechSynthesis.getVoices().length) {
    play();
  } else {
    let played = false;
    const playOnce = () => {
      if (played) return;
      played = true;
      play();
    };
    window.speechSynthesis.onvoiceschanged = playOnce;
    setTimeout(playOnce, 250);
  }
}

function playLocalAudio(text) {
  const audio = new Audio(`./assets/audio/${audioSlug(text)}.wav`);
  if (state.currentAudio) {
    state.currentAudio.pause();
    state.currentAudio.currentTime = 0;
  }
  state.currentAudio = audio;
  audio.play().catch(() => {});
}

function audioSlug(text) {
  const slug = text.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");
  return slug.length > 80 ? slug.slice(0, 80).replace(/-+$/g, "") : slug;
}

function normalize(text) {
  return text.toLowerCase().replace(/[^a-z\s']/g, " ").replace(/\s+/g, " ").trim();
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  })[char]);
}

window.setRecognizedText = function setRecognizedText(text) {
  state.recognizing = false;
  $("#recordButton").disabled = false;
  $("#recordButton").classList.remove("recording");
  $("#recordButton").textContent = "🎙 开始录入";
  input.value = text || "";
  input.placeholder = text ? "识别完成，可继续修改或点击完成朗读。" : "未识别到内容，请重试或直接输入英文。";
};

window.setRecognizeError = function setRecognizeError(message) {
  state.recognizing = false;
  $("#recordButton").disabled = false;
  $("#recordButton").classList.remove("recording");
  $("#recordButton").textContent = "🎙 开始录入";
  input.placeholder = message || "识别失败，请重试或直接输入英文。";
};

init();
