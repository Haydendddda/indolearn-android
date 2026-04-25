export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    const cors = { 'Access-Control-Allow-Origin': '*', 'Cache-Control': 'no-cache' };

    if (path === '/indolearn/version.json') {
      return new Response(JSON.stringify({
        version_code: 6, version: "1.0.6",
        apk_url: "https://1232131.xyz/indolearn/app.apk",
        changelog: "修复发音：在线 TTS 降级，适配中国版 Android 手机"
      }, null, 2), { headers: { ...cors, 'Content-Type': 'application/json' } });
    }

    if (path === '/indolearn/app.apk') {
      return Response.redirect('https://github.com/Haydendddda/indolearn-android/releases/download/v1.0.6/IndoLearn-v1.0.6.apk', 302);
    }

    if (path === '/indolearn/tts') {
      const text = url.searchParams.get('text') || '';
      const lang = url.searchParams.get('lang') || 'id';
      if (!text) return new Response('Missing text', { status: 400 });
      const ttsUrl = 'https://translate.google.com/translate_tts?ie=UTF-8&q=' + encodeURIComponent(text) + '&tl=' + lang + '&client=tw-ob';
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
