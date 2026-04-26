export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    const cors = { 'Access-Control-Allow-Origin': '*', 'Cache-Control': 'no-cache' };
    const html = (body, title='1232131.xyz') => new Response(
      `<!DOCTYPE html><html lang="zh"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title}</title>${STYLE}</head><body>${body}</body></html>`,
      { headers: { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-cache' } }
    );

    // ── IndoLearn API endpoints ──────────────────────────────────────────────

    if (path === '/indolearn/version.json') {
      return new Response(JSON.stringify({
        version_code: 14,
        version: "1.0.14",
        apk_url: "https://1232131.xyz/indolearn/app.apk",
        changelog: "v1.0.14: 统计模块全面升级（等级系统、热力图、最难单词）；优化授权页面 UX；修复更新时序"
      }, null, 2), { headers: { ...cors, 'Content-Type': 'application/json' } });
    }

    if (path === '/indolearn/app.apk') {
      const apkUrl = 'https://github.com/Haydendddda/indolearn-android/releases/download/v1.0.14/IndoLearn-v1.0.14.apk';
      try {
        const resp = await fetch(apkUrl, { redirect: 'follow' });
        if (!resp.ok) return new Response('APK fetch failed: ' + resp.status, { status: 502 });
        return new Response(resp.body, {
          headers: {
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Disposition': 'attachment; filename="IndoLearn-v1.0.14.apk"',
            'Access-Control-Allow-Origin': '*',
            'Cache-Control': 'public, max-age=86400'
          }
        });
      } catch (e) { return new Response('APK error: ' + e.message, { status: 502 }); }
    }

    if (path === '/indolearn/tts') {
      const text = url.searchParams.get('text') || '';
      const lang = url.searchParams.get('lang') || 'id';
      if (!text) return new Response('Missing text', { status: 400 });
      const ttsUrl = 'https://translate.google.com/translate_tts?ie=UTF-8&q='
        + encodeURIComponent(text) + '&tl=' + lang + '&client=tw-ob';
      try {
        const resp = await fetch(ttsUrl, { headers: { 'User-Agent': 'Mozilla/5.0 (compatible)' } });
        return new Response(resp.body, {
          headers: { 'Content-Type': 'audio/mpeg', 'Cache-Control': 'public, max-age=86400', 'Access-Control-Allow-Origin': '*' }
        });
      } catch (e) { return new Response('TTS error: ' + e.message, { status: 502 }); }
    }

    // ── IndoLearn landing page ───────────────────────────────────────────────

    if (path === '/indolearn' || path === '/indolearn/') {
      return html(`
        <a class="back" href="/">← 返回导航</a>
        <div class="app-hero">
          <div class="app-icon">🇮🇩</div>
          <div>
            <h1 class="app-name">IndoLearn</h1>
            <p class="app-tagline">AI 驱动的印尼语词汇学习 Android 应用</p>
            <span class="badge">v1.0.14</span>
            <span class="badge badge-green">免费开源</span>
          </div>
        </div>

        <div class="section">
          <h2>📱 应用介绍</h2>
          <p>IndoLearn 是一款专为中国用户设计的印尼语词汇学习工具。通过 AI 自动从你的 Gmail、PDF 或任意文本中提取印尼语词汇，配合间隔重复算法帮助你高效记忆。</p>
        </div>

        <div class="features">
          <div class="feature-card">
            <div class="feature-icon">✉️</div>
            <h3>Gmail 自动导入</h3>
            <p>连接 Gmail，AI 自动扫描邮件提取印尼语词汇并给出中文解释</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">📄</div>
            <h3>PDF / 文本导入</h3>
            <p>粘贴任意文本或上传 PDF，一键提取所有生词</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🧠</div>
            <h3>间隔重复</h3>
            <p>基于 SM-2 算法安排复习，每天几分钟，高效巩固记忆</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🔊</div>
            <h3>原生 TTS 发音</h3>
            <p>调用 Android 系统 TextToSpeech 引擎，准确的印尼语发音</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">☁️</div>
            <h3>Google Drive 同步</h3>
            <p>词库数据备份到你的私有 Drive 应用文件夹，换机不丢数据</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🤖</div>
            <h3>Gemini AI 支持</h3>
            <p>接入 Google Gemini API，智能分析词汇语境和用法</p>
          </div>
        </div>

        <div class="section download-section">
          <h2>⬇️ 下载安装</h2>
          <p style="color:var(--text2);margin-bottom:20px">支持 Android 7.0+，需要开启「允许安装未知来源应用」</p>
          <div class="download-btns">
            <a class="btn-dl btn-primary-dl" href="/indolearn/app.apk">⬇️ 下载 APK（v1.0.14）</a>
            <a class="btn-dl btn-ghost-dl" href="https://github.com/Haydendddda/indolearn-android/releases" target="_blank">📦 GitHub Releases</a>
          </div>
          <p style="font-size:12px;color:var(--text2);margin-top:14px">APK 经过 Cloudflare 代理，国内网络可直接下载</p>
        </div>

        <div class="section">
          <h2>🛠️ 技术栈</h2>
          <p>Kotlin · Android WebView · Google OAuth2 · Gemini API · Google Drive API · Gmail API · Cloudflare Workers · GitHub Actions 自动构建</p>
        </div>

        <div class="section">
          <h2>📋 更新日志</h2>
          <div style="display:flex;flex-direction:column;gap:12px">
            <div style="border-left:3px solid var(--accent);padding:8px 14px;background:var(--bg2);border-radius:0 8px 8px 0">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
                <span class="badge" style="margin:0">v1.0.14</span>
                <span style="font-size:12px;color:var(--text2)">最新版本</span>
              </div>
              <p style="margin:0;font-size:14px;color:var(--text)">统计模块全面升级（等级系统、7天热力图、最难单词分析）；优化 Google 授权页面 UX；修复 APK 更新时序问题</p>
            </div>
            <div style="border-left:3px solid var(--border);padding:8px 14px;background:var(--bg2);border-radius:0 8px 8px 0">
              <div style="margin-bottom:4px"><span class="badge" style="margin:0">v1.0.13</span></div>
              <p style="margin:0;font-size:14px;color:var(--text2)">修复本地备份导出（保存到下载文件夹）；修复导入文件选择器</p>
            </div>
            <div style="border-left:3px solid var(--border);padding:8px 14px;background:var(--bg2);border-radius:0 8px 8px 0">
              <div style="margin-bottom:4px"><span class="badge" style="margin:0">v1.0.12</span></div>
              <p style="margin:0;font-size:14px;color:var(--text2)">卡片可直接左右滑动（无需先翻转）；词库新增删除单词功能</p>
            </div>
            <div style="border-left:3px solid var(--border);padding:8px 14px;background:var(--bg2);border-radius:0 8px 8px 0">
              <div style="margin-bottom:4px"><span class="badge" style="margin:0">v1.0.11</span></div>
              <p style="margin:0;font-size:14px;color:var(--text2)">默认打开学习页；设置中新增每 24 小时自动同步 Gmail 选项；记忆技巧显示优化</p>
            </div>
            <div style="border-left:3px solid var(--border);padding:8px 14px;background:var(--bg2);border-radius:0 8px 8px 0">
              <div style="margin-bottom:4px"><span class="badge" style="margin:0">v1.0.10</span></div>
              <p style="margin:0;font-size:14px;color:var(--text2)">修复 Google Drive 云同步按钮无法点击的问题；新增导航主页与应用落地页</p>
            </div>
            <div style="border-left:3px solid var(--border);padding:8px 14px;background:var(--bg2);border-radius:0 8px 8px 0">
              <div style="margin-bottom:4px"><span class="badge" style="margin:0">v1.0.9</span></div>
              <p style="margin:0;font-size:14px;color:var(--text2)">修复 386 处乱码（HTML 双重编码问题根因修复）；应用内自动更新支持</p>
            </div>
            <div style="border-left:3px solid var(--border);padding:8px 14px;background:var(--bg2);border-radius:0 8px 8px 0">
              <div style="margin-bottom:4px"><span class="badge" style="margin:0">v1.0.6</span></div>
              <p style="margin:0;font-size:14px;color:var(--text2)">新增 TTS 语音朗读；Cloudflare Worker 代理发音 API；稳定签名 keystore</p>
            </div>
          </div>
          <p style="margin-top:14px;font-size:12px;color:var(--text2)">
            <a href="https://github.com/Haydendddda/indolearn-android/releases" target="_blank" style="color:var(--accent)">查看全部发布记录 →</a>
          </p>
        </div>
      `, 'IndoLearn — AI 印尼语学习');
    }

    // ── Navigation homepage ──────────────────────────────────────────────────

    if (path === '/' || path === '') {
      return html(`
        <header class="site-header">
          <h1>🛠️ Hayden 的应用</h1>
          <p class="site-sub">个人项目合集 · 持续更新中</p>
        </header>

        <div class="app-grid">
          <a class="app-card" href="/indolearn/">
            <div class="card-icon">🇮🇩</div>
            <div class="card-body">
              <div class="card-name">IndoLearn</div>
              <div class="card-desc">AI 驱动的印尼语词汇学习 Android 应用，从 Gmail / PDF 自动提取生词，配合间隔重复算法高效记忆</div>
              <div class="card-tags">
                <span class="tag">Android</span>
                <span class="tag">AI</span>
                <span class="tag">Kotlin</span>
                <span class="tag">印尼语</span>
              </div>
            </div>
            <div class="card-arrow">→</div>
          </a>
        </div>

        <footer class="site-footer">
          <p>更多应用陆续上线 · <a href="https://github.com/Haydendddda" target="_blank">GitHub</a></p>
        </footer>
      `, '1232131.xyz — Hayden 的应用');
    }

    return new Response('Not Found', { status: 404 });
  }
};

const STYLE = `<style>
  :root {
    --bg: #0f1117;
    --bg2: #1a1d27;
    --bg3: #22263a;
    --accent: #4f7cff;
    --accent2: #34c759;
    --text: #e8eaf0;
    --text2: #8b92a8;
    --border: #2a2d3e;
    --radius: 14px;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, 'PingFang SC', 'Helvetica Neue', sans-serif;
    background: var(--bg);
    color: var(--text);
    min-height: 100vh;
    padding: 24px 16px 60px;
    max-width: 860px;
    margin: 0 auto;
    line-height: 1.6;
  }
  a { color: var(--accent); text-decoration: none; }
  /* Header */
  .site-header { text-align: center; padding: 48px 0 40px; }
  .site-header h1 { font-size: 2rem; font-weight: 700; }
  .site-sub { color: var(--text2); margin-top: 8px; }
  /* App grid */
  .app-grid { display: flex; flex-direction: column; gap: 16px; }
  .app-card {
    display: flex; align-items: flex-start; gap: 18px;
    background: var(--bg2); border: 1px solid var(--border);
    border-radius: var(--radius); padding: 22px; cursor: pointer;
    transition: border-color .2s, transform .15s;
    color: var(--text); text-decoration: none;
  }
  .app-card:hover { border-color: var(--accent); transform: translateY(-2px); }
  .card-icon { font-size: 2.5rem; flex-shrink: 0; }
  .card-body { flex: 1; }
  .card-name { font-size: 1.15rem; font-weight: 700; margin-bottom: 6px; }
  .card-desc { font-size: 0.88rem; color: var(--text2); margin-bottom: 12px; }
  .card-tags { display: flex; flex-wrap: wrap; gap: 6px; }
  .tag { background: var(--bg3); border: 1px solid var(--border); border-radius: 20px; padding: 2px 10px; font-size: 11px; color: var(--text2); }
  .card-arrow { font-size: 1.4rem; color: var(--text2); align-self: center; }
  /* Footer */
  .site-footer { text-align: center; margin-top: 60px; color: var(--text2); font-size: 13px; }
  /* Landing page */
  .back { display: inline-block; color: var(--text2); font-size: 14px; margin-bottom: 24px; }
  .back:hover { color: var(--text); }
  .app-hero { display: flex; align-items: center; gap: 20px; margin-bottom: 40px; flex-wrap: wrap; }
  .app-icon { font-size: 4rem; }
  .app-name { font-size: 2rem; font-weight: 700; }
  .app-tagline { color: var(--text2); margin: 4px 0 12px; }
  .badge { display: inline-block; background: var(--bg3); border: 1px solid var(--border); border-radius: 20px; padding: 2px 12px; font-size: 12px; color: var(--text2); margin-right: 6px; }
  .badge-green { border-color: #34c75944; color: var(--accent2); background: #34c75910; }
  .section { background: var(--bg2); border: 1px solid var(--border); border-radius: var(--radius); padding: 24px; margin-bottom: 20px; }
  .section h2 { font-size: 1.05rem; font-weight: 700; margin-bottom: 12px; }
  .section p { color: var(--text2); font-size: 0.9rem; }
  .features { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; margin-bottom: 20px; }
  .feature-card { background: var(--bg2); border: 1px solid var(--border); border-radius: var(--radius); padding: 20px; }
  .feature-icon { font-size: 1.6rem; margin-bottom: 10px; }
  .feature-card h3 { font-size: 0.95rem; font-weight: 600; margin-bottom: 6px; }
  .feature-card p { font-size: 0.82rem; color: var(--text2); }
  .download-section { text-align: center; }
  .download-btns { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }
  .btn-dl { display: inline-block; padding: 13px 28px; border-radius: 10px; font-size: 0.95rem; font-weight: 600; cursor: pointer; }
  .btn-primary-dl { background: var(--accent); color: #fff; }
  .btn-primary-dl:hover { background: #3d6ee0; }
  .btn-ghost-dl { background: var(--bg3); color: var(--text); border: 1px solid var(--border); }
  .btn-ghost-dl:hover { border-color: var(--accent); }
  @media (max-width: 600px) {
    .app-hero { gap: 14px; }
    .app-icon { font-size: 2.8rem; }
    .app-name { font-size: 1.5rem; }
  }
</style>`;
