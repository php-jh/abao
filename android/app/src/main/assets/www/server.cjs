const http = require("http");
const fs = require("fs");
const path = require("path");

const root = __dirname;
const port = Number(process.env.PORT || 5173);
const types = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".png": "image/png",
  ".wav": "audio/wav"
};

const server = http.createServer((req, res) => {
  if (req.method === "POST" && req.url.startsWith("/api/asr")) {
    handleAsr(req, res);
    return;
  }

  const urlPath = decodeURIComponent(new URL(req.url, `http://localhost:${port}`).pathname);
  const cleanPath = urlPath.replace(/^[/\\]+/, "") || "index.html";
  const safePath = path.normalize(cleanPath).replace(/^(\.\.[/\\])+/, "");
  const filePath = path.join(root, safePath);

  if (!filePath.startsWith(root)) {
    res.writeHead(403);
    res.end("Forbidden");
    return;
  }

  fs.readFile(filePath, (error, content) => {
    if (error) {
      res.writeHead(404);
      res.end("Not found");
      return;
    }
    res.writeHead(200, { "Content-Type": types[path.extname(filePath)] || "application/octet-stream" });
    res.end(content);
  });
});

server.listen(port, "127.0.0.1", () => {
  console.log(`Po speaking demo: http://127.0.0.1:${port}`);
});

async function handleAsr(req, res) {
  try {
    const appkey = process.env.ALIYUN_NLS_APPKEY;
    const token = process.env.ALIYUN_NLS_TOKEN;
    if (!appkey || !token) {
      sendJson(res, 501, {
        error: "ALIYUN_NOT_CONFIGURED",
        message: "服务端未配置 ALIYUN_NLS_APPKEY / ALIYUN_NLS_TOKEN。"
      });
      return;
    }

    const audio = await readBody(req, 12 * 1024 * 1024);
    if (!audio.length) {
      sendJson(res, 400, { error: "EMPTY_AUDIO", message: "没有收到音频数据。" });
      return;
    }

    const params = new URLSearchParams({
      appkey,
      format: "wav",
      sample_rate: "16000",
      enable_punctuation_prediction: "true",
      enable_inverse_text_normalization: "true"
    });
    const endpoint = `https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/asr?${params.toString()}`;
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "X-NLS-Token": token,
        "Content-Type": "application/octet-stream"
      },
      body: audio
    });
    const raw = await response.text();
    let data;
    try {
      data = JSON.parse(raw);
    } catch {
      data = { raw };
    }
    if (!response.ok || data.status !== 20000000) {
      sendJson(res, 502, {
        error: "ALIYUN_ASR_FAILED",
        message: data.message || "阿里云语音识别失败。",
        raw: data
      });
      return;
    }
    sendJson(res, 200, { text: data.result || "", raw: data });
  } catch (error) {
    sendJson(res, 500, { error: "SERVER_ERROR", message: error.message });
  }
}

function readBody(req, limit) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on("data", (chunk) => {
      size += chunk.length;
      if (size > limit) {
        reject(new Error("音频文件过大。"));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

function sendJson(res, status, payload) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(payload));
}
