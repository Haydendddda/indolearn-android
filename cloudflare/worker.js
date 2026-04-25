export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    const cors = { 'Access-Control-Allow-Origin': '*', 'Cache-Control': 'no-cache' };

    if (path === '/indolearn/version.json') {
      return new Response(JSON.stringify({
        version_code: 7,
        version: "1.0.7",
        apk_url: "https://1232131.xyz/indolearn/app.apk",
        changelog: "修复乱码：解决部分国产 Android（如 OPPO）显示乱码；修复自动更新安装失败"
      }, null, 2), { headers: { ...cors, 'Content-Type': 'application/json' } });
    }

    if (path === '/indolearn/app.apk') {
      return Response.redirect(
        'https://github.com/Haydendddda/indolearn-android/releases/download/v1.0.7/IndoLearn-v1.0.7.apk',
        302
      );
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
