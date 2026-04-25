export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    const cors = { 'Access-Control-Allow-Origin': '*', 'Cache-Control': 'no-cache' };

    if (path === '/indolearn/version.json') {
      return new Response(JSON.stringify({
        version_code: 8,
        version: "1.0.8",
        apk_url: "https://1232131.xyz/indolearn/app.apk",
        changelog: "彻底修复乱码：改用显式 UTF-8 解码，解决 OPPO/MIUI WebView 乱码"
      }, null, 2), { headers: { ...cors, 'Content-Type': 'application/json' } });
    }

    // Proxy the APK directly — do NOT redirect to GitHub.
    // GitHub is inaccessible in mainland China, so the Worker fetches the file
    // on behalf of the user and streams it through Cloudflare's network.
    if (path === '/indolearn/app.apk') {
      const apkUrl = 'https://github.com/Haydendddda/indolearn-android/releases/download/v1.0.8/IndoLearn-v1.0.8.apk';
      try {
        const resp = await fetch(apkUrl, { redirect: 'follow' });
        if (!resp.ok) {
          return new Response('APK fetch failed: ' + resp.status, { status: 502 });
        }
        return new Response(resp.body, {
          headers: {
            'Content-Type': 'application/vnd.android.package-archive',
            'Content-Disposition': 'attachment; filename="IndoLearn-v1.0.8.apk"',
            'Access-Control-Allow-Origin': '*',
            'Cache-Control': 'public, max-age=86400'
          }
        });
      } catch (e) {
        return new Response('APK error: ' + e.message, { status: 502 });
      }
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
          headers: {
            'Content-Type': 'audio/mpeg',
            'Cache-Control': 'public, max-age=86400',
            'Access-Control-Allow-Origin': '*'
          }
        });
      } catch (e) {
        return new Response('TTS error: ' + e.message, { status: 502 });
      }
    }

    if (path === '/indolearn' || path === '/indolearn/') {
      return Response.redirect('https://github.com/Haydendddda/indolearn-android/releases/latest', 302);
    }

    return new Response('Not Found', { status: 404 });
  }
};
