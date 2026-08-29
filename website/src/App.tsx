import { useEffect, useLayoutEffect, useRef, useState, type ReactNode } from 'react';

type Language = 'en' | 'pt';
type Route = '/' | '/privacy' | '/support';
type FeatureIconName = 'lockscreen' | 'quiz' | 'widget' | 'audio' | 'quran' | 'progress';

const artworkRevision = '0.30.5-2026-08-25';

const facts = {
  email: 'uthman-al-brazili@proton.me',
  support: 'mailto:uthman-al-brazili@proton.me?subject=Supporting%20Kalima',
  repository: 'https://github.com/uthman-al-brazili/kalima',
  release: 'https://github.com/uthman-al-brazili/kalima/releases/tag/v0.32.0',
  apk: 'https://github.com/uthman-al-brazili/kalima/releases/download/v0.32.0/kalima-0.32.0-release.apk',
  obtainium: 'https://github.com/ImranR98/Obtainium/releases',
};

const featureIconPaths: Record<Exclude<FeatureIconName, 'lockscreen'>, string> = {
  quiz: 'M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,19h-2v-2h2v2zM15.07,11.25l-0.9,0.92C13.45,12.9 13,13.5 13,15h-2v-0.5c0,-1.1 0.45,-2.1 1.17,-2.83l1.24,-1.26c0.37,-0.36 0.59,-0.86 0.59,-1.41a2,2 0,0 0,-4 0H8a4,4 0,0 1,8 0c0,0.88 -0.36,1.68 -0.93,2.25z',
  widget: 'M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6zM20,2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM19,11H9V9h10v2zM15,15H9v-2h6v2zM19,7H9V5h10v2z',
  audio: 'M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z',
  quran: 'M4,3h6c1.1,0 2,0.9 2,2v15c-0.6,-0.7 -1.5,-1 -2.5,-1H4c-1.1,0 -2,-0.9 -2,-2V5c0,-1.1 0.9,-2 2,-2zM20,3h-6c-1.1,0 -2,0.9 -2,2v15c0.6,-0.7 1.5,-1 2.5,-1H20c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM10,7H5v2h5V7zM19,7h-5v2h5V7zM10,11H5v2h5v-2zM19,11h-5v2h5v-2z',
  progress: 'M3,13h2v8H3v-8zM7,9h2v12H7V9zM11,3h2v18h-2V3zM15,11h2v10h-2V11zM19,6h2v15h-2V6z',
};

const stepIcons: readonly FeatureIconName[] = ['lockscreen', 'quiz', 'quran'];

const content = {
  en: {
    seo: { title: 'Kalima — Learn Quranic Arabic on your lock screen', description: 'Learn Quranic Arabic on your lock screen when your screen turns on. Kalima is private, offline-first, and never bypasses your PIN, password, or biometrics.' },
    nav: { learn: 'Lock-screen learning', features: 'Features', privacy: 'Privacy', support: 'Support', cta: 'Get Kalima' },
    hero: {
      eyebrow: 'Quranic Arabic • On your lock screen',
      title: 'Learn Quranic Arabic before you unlock.',
      body: 'Turn screen-on moments into short Quranic Arabic lessons. Kalima keeps your device locked and never bypasses your PIN, password, or biometrics.',
      cta: 'Get Kalima',
      secondary: 'See lock-screen learning',
      note: 'Free • No ads • Android 8+',
    },
    card: { label: 'Lock-screen word', date: 'Tuesday, August 24', security: 'Device stays locked', transliteration: 'hudā', meaning: 'guidance', again: 'Again', gotIt: 'Got it', ready: 'Choose how this word felt.', saved: 'Nice. Kalima will bring it back at the right time.', retry: 'No pressure. This word will return sooner.' },
    proof: [
      ['Screen-on', 'Quranic Arabic practice'],
      ['42,117', 'offline study cards'],
      ['0', 'ads or trackers'],
    ],
    promise: 'Make every screen-on moment count.',
    promiseBody: 'Kalima brings short Quranic Arabic practice to the lock screen, then connects each word to its Quran context.',
    steps: [
      ['01', 'Meet a Quranic Arabic word', 'See the Arabic, transliteration, pronunciation, and meaning directly on your lock screen.'],
      ['02', 'Recall it with a quick quiz', 'Answer listening, context, cloze, and root questions when the screen turns on.'],
      ['03', 'Continue in Quran context', 'Open the exact lesson, listen to real Quran audio, and explore the ayah, grammar, root, and related occurrences.'],
    ],
    screensTitle: 'From the lock screen into Quran context.',
    screensBody: 'Start with a screen-on card, then continue with focused word study, Quran reading, and Arabic foundations.',
    screens: [
      ['lock-screen-learning.png', 'Quranic Arabic word shown on the lock screen inside a simulated Android phone', 'Review a Quranic Arabic word at screen-on while Android keeps your PIN and biometrics in control.'],
      ['word-study.png', 'Kalima daily Quranic word study screen with Arabic, pronunciation, and review controls', 'Build vocabulary one word at a time with pronunciation, contextual meaning, and spaced review.'],
      ['quran-reading.png', 'Kalima Quran reader with tappable Arabic words', 'Tap any word anywhere in the Quran to connect vocabulary with its ayah and deeper context.'],
      ['arabic-foundations.png', 'Kalima Arabic basics screen with alphabet and Arabic-Indic number lessons', 'Practice the alphabet and Arabic-Indic numbers separately whenever you need the foundation.'],
    ],
    featuresTitle: 'Built around small moments that add up.',
    features: [
      ['lockscreen', 'Lock-screen lessons', 'Practice when the screen turns on while Android keeps the device keyguard fully in control.'],
      ['quiz', 'Quick lock-screen quizzes', 'Listening, context, cloze, and root questions turn brief moments into active recall.'],
      ['widget', 'Daily Quran word widget', 'See one word at a glance and open its exact lesson without altering your progress.'],
      ['audio', 'Real Quran audio', 'Hear word recordings and Al-Hussary’s complete-ayah recitation. Played audio can be saved locally.'],
      ['quran', 'Context, not isolation', 'Open the ayah, inspect roots and grammar, and find other indexed occurrences offline.'],
      ['progress', 'Spaced repetition', 'Review what is due instead of restarting from the beginning.'],
    ],
    trustTitle: 'Your learning belongs to you.',
    trustBody: 'No account, advertising, analytics, or remote learning database. Progress stays on your device and can be exported manually.',
    trustLinks: ['Read the privacy policy', 'See support details'],
    availability: {
      kicker: 'Kalima for Android',
      title: 'Turn unlock moments into Quranic Arabic practice.',
      body: 'Download the official signed APK from GitHub. Kalima is free, bilingual, private, and built for offline-first study.',
      cta: 'Download Kalima 0.32.0',
      secondary: 'View the GitHub release',
      installKicker: 'Updates through GitHub',
      installTitle: 'Install once. Keep future updates simple.',
      installBody: 'The GitHub app does not update installed Android apps by itself. Obtainium can watch Kalima’s GitHub Releases, check for new versions automatically, and guide you through each update.',
      installSteps: [
        ['01', 'Install the signed APK', 'Download Kalima above and allow your browser to install unknown apps when Android asks.'],
        ['02', 'Install Obtainium', 'Get Obtainium from its official GitHub Releases page and allow it to install unknown apps.'],
        ['03', 'Add the Kalima repository', 'Paste the repository address into Obtainium. It will check for releases automatically; Android normally asks you to confirm each installation.'],
      ],
      obtainium: 'Open Obtainium releases',
      repositoryLabel: 'Repository address for Obtainium',
      warning: 'Already using an older debug build? Export any progress you want to keep, uninstall that build once, then install this signed release. Future signed Kalima releases can update it normally.',
    },
    footer: 'Independent, thoughtful software for Quranic Arabic learners.',
    legal: 'Kalima is a learning aid and does not replace a scholarly translation or qualified religious instruction.',
  },
  pt: {
    seo: { title: 'Kalima — Aprenda árabe corânico na tela bloqueada', description: 'Aprenda árabe corânico na tela bloqueada quando a tela acende. O Kalima prioriza a privacidade e nunca contorna seu PIN, senha ou biometria.' },
    nav: { learn: 'Tela bloqueada', features: 'Recursos', privacy: 'Privacidade', support: 'Suporte', cta: 'Conheça o Kalima' },
    hero: {
      eyebrow: 'Árabe corânico • Na tela bloqueada',
      title: 'Aprenda árabe corânico antes de desbloquear.',
      body: 'Transforme o acender da tela em lições curtas de árabe corânico. O Kalima mantém o aparelho bloqueado e nunca contorna seu PIN, senha ou biometria.',
      cta: 'Conheça o Kalima',
      secondary: 'Veja o aprendizado na tela bloqueada',
      note: 'Grátis • Sem anúncios • Android 8+',
    },
    card: { label: 'Palavra na tela bloqueada', date: 'Terça-feira, 24 de agosto', security: 'Aparelho permanece bloqueado', transliteration: 'hudā', meaning: 'orientação', again: 'De novo', gotIt: 'Entendi', ready: 'Como esta palavra pareceu?', saved: 'Muito bem. O Kalima vai trazê-la de volta no momento certo.', retry: 'Sem pressão. Esta palavra voltará mais cedo.' },
    proof: [
      ['Ao acender', 'prática de árabe corânico'],
      ['42.117', 'cartões offline'],
      ['0', 'anúncios ou rastreadores'],
    ],
    promise: 'Faça cada acender da tela valer a pena.',
    promiseBody: 'O Kalima leva práticas curtas de árabe corânico à tela bloqueada e conecta cada palavra ao contexto do Alcorão.',
    steps: [
      ['01', 'Conheça uma palavra do Alcorão', 'Veja o árabe, a transliteração, a pronúncia e o significado diretamente na tela bloqueada.'],
      ['02', 'Relembre com um quiz rápido', 'Responda a questões de áudio, contexto, lacunas e raízes quando a tela acender.'],
      ['03', 'Continue no contexto do Alcorão', 'Abra a lição exata, ouça áudio real e explore a ayah, a gramática, a raiz e ocorrências relacionadas.'],
    ],
    screensTitle: 'Da tela bloqueada ao contexto do Alcorão.',
    screensBody: 'Comece com um cartão ao acender a tela e continue com estudo de palavras, leitura do Alcorão e fundamentos do árabe.',
    screens: [
      ['lock-screen-learning.png', 'Palavra do Alcorão exibida na tela bloqueada dentro de um telefone Android simulado', 'Revise uma palavra do Alcorão ao acender a tela, com o PIN e a biometria sob o controle do Android.'],
      ['word-study.png', 'Tela do Kalima para estudar a palavra corânica do dia, com áudio e controles de revisão', 'Construa vocabulário uma palavra de cada vez, com pronúncia, significado em contexto e revisão espaçada.'],
      ['quran-reading.png', 'Leitor do Alcorão do Kalima com palavras árabes tocáveis', 'Toque em qualquer palavra, em qualquer parte do Alcorão, para conectá-la à ayah e ao seu contexto.'],
      ['arabic-foundations.png', 'Tela de fundamentos do árabe do Kalima com alfabeto e números indo-arábicos', 'Pratique o alfabeto e os números indo-arábicos separadamente quando precisar reforçar a base.'],
    ],
    featuresTitle: 'Pequenos momentos que constroem conhecimento.',
    features: [
      ['lockscreen', 'Lições na tela bloqueada', 'Pratique quando a tela acender enquanto o bloqueio do Android continua totalmente no controle.'],
      ['quiz', 'Quizzes rápidos na tela bloqueada', 'Questões de áudio, contexto, lacunas e raízes transformam momentos breves em recordação ativa.'],
      ['widget', 'Widget da palavra diária', 'Veja uma palavra num relance e abra sua lição exata sem alterar seu progresso.'],
      ['audio', 'Áudio real do Alcorão', 'Ouça palavras e a recitação de ayahs completas por Al-Hussary. Os áudios tocados podem ser salvos.'],
      ['quran', 'Palavras em contexto', 'Abra a ayah, consulte raiz e gramática e encontre outras ocorrências offline.'],
      ['progress', 'Repetição espaçada', 'Revise o que está pendente em vez de sempre recomeçar do início.'],
    ],
    trustTitle: 'Seu aprendizado pertence a você.',
    trustBody: 'Sem conta, publicidade, análises ou banco remoto de atividades. O progresso fica no aparelho e pode ser exportado manualmente.',
    trustLinks: ['Leia a política de privacidade', 'Veja os detalhes de suporte'],
    availability: {
      kicker: 'Kalima para Android',
      title: 'Transforme o desbloqueio em prática de árabe corânico.',
      body: 'Baixe o APK oficial assinado pelo GitHub. O Kalima é gratuito, bilíngue, privado e feito para estudar mesmo sem internet.',
      cta: 'Baixar Kalima 0.32.0',
      secondary: 'Ver a versão no GitHub',
      installKicker: 'Atualizações pelo GitHub',
      installTitle: 'Instale uma vez. Simplifique as próximas atualizações.',
      installBody: 'O aplicativo do GitHub não atualiza sozinho os apps instalados no Android. O Obtainium pode acompanhar as versões do Kalima no GitHub, verificar novidades automaticamente e orientar cada atualização.',
      installSteps: [
        ['01', 'Instale o APK assinado', 'Baixe o Kalima acima e permita que o navegador instale apps desconhecidos quando o Android solicitar.'],
        ['02', 'Instale o Obtainium', 'Baixe o Obtainium na página oficial de versões do GitHub e permita que ele instale apps desconhecidos.'],
        ['03', 'Adicione o repositório do Kalima', 'Cole o endereço do repositório no Obtainium. Ele verificará novas versões automaticamente; normalmente o Android pedirá sua confirmação para cada instalação.'],
      ],
      obtainium: 'Abrir versões do Obtainium',
      repositoryLabel: 'Endereço do repositório para o Obtainium',
      warning: 'Já usa uma versão debug antiga? Exporte o progresso que deseja manter, desinstale essa versão uma vez e instale o APK assinado. As próximas versões assinadas do Kalima poderão ser instaladas como atualização.',
    },
    footer: 'Software independente e cuidadoso para estudantes de árabe corânico.',
    legal: 'O Kalima é um auxílio de aprendizagem e não substitui uma tradução acadêmica ou orientação religiosa qualificada.',
  },
} as const;

function normalizePath(pathname: string): Route {
  if (pathname.startsWith('/privacy')) return '/privacy';
  if (pathname.startsWith('/support')) return '/support';
  return '/';
}

function SmartLink({ href, children, className, onClick }: { href: string; children: ReactNode; className?: string; onClick?: () => void }) {
  const handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    onClick?.();
    if (!href.startsWith('/')) return;
    event.preventDefault();
    window.history.pushState({}, '', href);
    window.dispatchEvent(new PopStateEvent('popstate'));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  return <a href={href} className={className} onClick={handleClick}>{children}</a>;
}

function FeatureIcon({ name }: { name: FeatureIconName }) {
  if (name === 'lockscreen') {
    return (
      <svg viewBox="0 0 24 24" role="presentation" focusable="false">
        <rect x="3" y="1.5" width="14.5" height="21" rx="3" fill="currentColor" />
        <rect x="5" y="4" width="10.5" height="16" rx="1.5" fill="white" fillOpacity="0.82" />
        <text x="10.25" y="14.5" fill="currentColor" fontFamily="'Noto Naskh Arabic', 'Amiri', serif" fontSize="8.5" fontWeight="700" textAnchor="middle">ك</text>
        <circle cx="18.2" cy="17.8" r="4.3" fill="currentColor" />
        <path d="M16.85 17.1v-1a1.35 1.35 0 0 1 2.7 0v1m-3.35 0h4v3h-4z" fill="none" stroke="white" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.15" />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" role="presentation" focusable="false">
      <path d={featureIconPaths[name]} fill="currentColor" />
    </svg>
  );
}

function App() {
  const [language, setLanguage] = useState<Language>(() => {
    try {
      const stored = localStorage.getItem('kalima-language');
      if (stored === 'en' || stored === 'pt') return stored;
    } catch {
      // The language switch still works when browser storage is unavailable.
    }
    return navigator.language.toLowerCase().startsWith('pt') ? 'pt' : 'en';
  });
  const [route, setRoute] = useState<Route>(() => normalizePath(window.location.pathname));

  useEffect(() => {
    const updateRoute = () => setRoute(normalizePath(window.location.pathname));
    window.addEventListener('popstate', updateRoute);
    return () => window.removeEventListener('popstate', updateRoute);
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem('kalima-language', language);
    } catch {
      // Keep the current in-memory choice without requiring storage access.
    }
    document.documentElement.lang = language === 'pt' ? 'pt-BR' : 'en';
    const seo = content[language].seo;
    document.title = seo.title;
    document.querySelector<HTMLMetaElement>('meta[name="description"]')?.setAttribute('content', seo.description);
    document.querySelector<HTMLMetaElement>('meta[property="og:title"]')?.setAttribute('content', seo.title);
    document.querySelector<HTMLMetaElement>('meta[property="og:description"]')?.setAttribute('content', seo.description);
  }, [language]);

  return (
    <div className="site-shell">
      <a className="skip-link" href="#main">Skip to content</a>
      <Header language={language} setLanguage={setLanguage} route={route} />
      <main id="main">
        {route === '/' && <Home language={language} />}
        {route === '/privacy' && <Privacy language={language} />}
        {route === '/support' && <Support language={language} />}
      </main>
      <Footer language={language} />
    </div>
  );
}

function Header({ language, setLanguage, route }: { language: Language; setLanguage: (language: Language) => void; route: Route }) {
  const t = content[language];
  return (
    <header className="site-header">
      <div className="nav-wrap">
        <SmartLink href="/" className="brand">
          <img src="/kalima-icon.png" alt="" />
          <span>Kalima</span>
        </SmartLink>
        <nav className="desktop-nav" aria-label="Primary navigation">
          {route === '/' ? <>
            <a href="#how">{t.nav.learn}</a>
            <a href="#features">{t.nav.features}</a>
          </> : null}
          <SmartLink href="/privacy">{t.nav.privacy}</SmartLink>
          <SmartLink href="/support">{t.nav.support}</SmartLink>
        </nav>
        <div className="nav-actions">
          <div className="language-switch" aria-label="Language">
            <button className={language === 'en' ? 'active' : ''} onClick={() => setLanguage('en')} aria-pressed={language === 'en'}>EN</button>
            <button className={language === 'pt' ? 'active' : ''} onClick={() => setLanguage('pt')} aria-pressed={language === 'pt'}>PT</button>
          </div>
          <a className="button small dark desktop-cta" href={route === '/' ? '#availability' : '/#availability'}>{t.nav.cta}</a>
        </div>
      </div>
    </header>
  );
}

function StudyCard({ language }: { language: Language }) {
  const t = content[language].card;
  const [status, setStatus] = useState<'ready' | 'saved' | 'retry'>('ready');
  const phoneRef = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    const phone = phoneRef.current;
    if (!phone) return;

    const resizePhone = () => {
      phone.style.setProperty('--phone-scale', String(phone.getBoundingClientRect().width / 316));
    };
    const observer = new ResizeObserver(resizePhone);
    observer.observe(phone);
    resizePhone();

    return () => observer.disconnect();
  }, []);

  return (
    <div className="study-stage" aria-label={language === 'pt' ? 'Prévia interativa do aprendizado na tela bloqueada' : 'Interactive lock-screen learning preview'}>
      <span className="orbit-dot dot-one" />
      <span className="orbit-dot dot-two" />
      <div className="lockscreen-preview" ref={phoneRef}>
        <div className="lockscreen-canvas">
          <div className="lockscreen-status" aria-hidden="true"><span>9:44</span><span>● ◢ ▮</span></div>
          <span className="lockscreen-lock" aria-hidden="true" />
          <div className="lockscreen-time" aria-hidden="true">9:44</div>
          <div className="lockscreen-date">{t.date}</div>
          <div className="study-card">
            <div className="study-card-top"><span>{t.label}</span><span className="audio-mark" aria-hidden="true">◖</span></div>
            <div className="arabic" lang="ar" dir="rtl">هُدًى</div>
            <div className="transliteration">{t.transliteration}</div>
            <div className="meaning">{t.meaning}</div>
            <div className="card-divider" />
            <p className="card-status" aria-live="polite">{t[status]}</p>
            <div className="card-actions">
              <button className="study-button secondary" onClick={() => setStatus('retry')}>{t.again}</button>
              <button className="study-button primary" onClick={() => setStatus('saved')}>{t.gotIt}</button>
            </div>
          </div>
          <div className="lockscreen-security"><span className="security-lock" aria-hidden="true" />{t.security}</div>
        </div>
      </div>
    </div>
  );
}

function Home({ language }: { language: Language }) {
  const t = content[language];
  const artworkLocale = language === 'pt' ? 'pt-BR' : 'en';
  return <>
    <section className="hero section-pad">
      <div className="hero-copy">
        <div className="eyebrow"><span />{t.hero.eyebrow}</div>
        <h1>{t.hero.title}</h1>
        <p className="hero-body">{t.hero.body}</p>
        <div className="hero-actions">
          <a className="button gold" href="#availability">{t.hero.cta}<span aria-hidden="true">→</span></a>
          <a className="button ghost" href="#how">{t.hero.secondary}</a>
        </div>
        <p className="hero-note"><span aria-hidden="true">✓</span>{t.hero.note}</p>
      </div>
      <StudyCard language={language} />
    </section>

    <section className="proof-row section-pad" aria-label="Kalima facts">
      {t.proof.map(([value, label]) => <div className="proof-item" key={label}><strong>{value}</strong><span>{label}</span></div>)}
    </section>

    <section className="path-section section-pad">
      <div className="section-heading centered" id="how">
        <span className="section-kicker">{language === 'en' ? 'A path with purpose' : 'Um caminho com propósito'}</span>
        <h2>{t.promise}</h2>
        <p>{t.promiseBody}</p>
      </div>
      <div className="step-grid">
        {t.steps.map(([number, title, body], index) => <article className={`step-card step-${index + 1}`} key={number}>
          <span className="step-number">{number}</span>
          <div className="step-icon" aria-hidden="true"><FeatureIcon name={stepIcons[index]} /></div>
          <h3>{title}</h3><p>{body}</p>
        </article>)}
      </div>
    </section>

    <section className="screens-section" aria-labelledby="screens-title">
      <div className="section-pad screens-intro">
        <div className="section-heading"><span className="section-kicker">{language === 'en' ? 'Lock-screen learning' : 'Aprendizado na tela bloqueada'}</span><h2 id="screens-title">{t.screensTitle}</h2></div>
        <p>{t.screensBody}</p>
      </div>
      <div className="screen-rail section-pad">
        {t.screens.map(([image, alt, body], index) => <article className={`screen-card screen-tone-${index + 1}`} key={image}>
          <div className="phone-frame"><img src={`/screens/${artworkLocale}/${image}?v=${artworkRevision}`} alt={alt} width={1080} height={1350} loading="lazy" decoding="async" /></div>
          <div className="screen-copy"><span>0{index + 1}</span><p>{body}</p></div>
        </article>)}
      </div>
    </section>

    <section className="features-section section-pad">
      <div className="section-heading centered" id="features"><span className="section-kicker">{language === 'en' ? 'Built for real study' : 'Feito para estudar de verdade'}</span><h2>{t.featuresTitle}</h2></div>
      <div className="feature-grid">
        {t.features.map(([icon, title, body]) => <article className="feature-card" key={title}><span className="feature-icon" aria-hidden="true"><FeatureIcon name={icon} /></span><h3>{title}</h3><p>{body}</p></article>)}
      </div>
    </section>

    <section className="trust-wrap section-pad">
      <div className="trust-panel">
        <div className="privacy-seal" aria-hidden="true">
          <span>✓</span>
          <small>{language === 'en' ? <>PRIVATE<br />BY DESIGN</> : <>PRIVACIDADE<br />DESDE O INÍCIO</>}</small>
        </div>
        <div><span className="section-kicker light">{language === 'en' ? 'Privacy without fine print' : 'Privacidade sem letras miúdas'}</span><h2>{t.trustTitle}</h2><p>{t.trustBody}</p>
          <div className="inline-links"><SmartLink href="/privacy">{t.trustLinks[0]} →</SmartLink><SmartLink href="/support">{t.trustLinks[1]} →</SmartLink></div>
        </div>
      </div>
    </section>

    <section className="availability section-pad" id="availability">
      <div className="availability-card">
        <div><span className="section-kicker">{t.availability.kicker}</span><h2>{t.availability.title}</h2><p>{t.availability.body}</p>
          <div className="hero-actions"><a className="button dark" href={facts.apk}>{t.availability.cta}<span aria-hidden="true">↓</span></a><a className="button ghost" href={facts.release}>{t.availability.secondary}</a></div>
        </div>
        <img src="/kalima-icon.png" alt="Kalima app icon" />
        <div className="install-guide">
          <div className="install-guide-heading"><span className="section-kicker">{t.availability.installKicker}</span><h3>{t.availability.installTitle}</h3><p>{t.availability.installBody}</p></div>
          <ol className="install-steps">
            {t.availability.installSteps.map(([number, title, body]) => <li key={number}><span>{number}</span><div><h4>{title}</h4><p>{body}</p></div></li>)}
          </ol>
          <div className="update-actions">
            <a className="button gold" href={facts.obtainium}>{t.availability.obtainium}<span aria-hidden="true">↗</span></a>
            <div className="repository-address"><span>{t.availability.repositoryLabel}</span><code>{facts.repository}</code></div>
          </div>
          <p className="install-warning"><span aria-hidden="true">!</span>{t.availability.warning}</p>
        </div>
      </div>
    </section>
  </>;
}

const policy = {
  en: {
    eyebrow: 'Privacy policy', title: 'Clear, human privacy.', intro: 'Effective August 28, 2026. Kalima is an offline-first Quranic Arabic learning app developed by Uthman (Gustavo).',
    sections: [
      ['What stays on your device', 'Study progress and preferences are stored locally. This may include learned and reviewing words, study selections, foundation progress, reminders, downloaded audio, and display preferences. The developer does not receive this information.'],
      ['Backups you control', 'You may export a progress backup to a file location you choose and import it later. Kalima does not upload that backup. You control any external service or device location where you copy it.'],
      ['Network access and audio', 'Core lessons, study data, and Quran text are bundled for offline use. When you choose to stream or download Quran audio, word recordings come from QuranCDN and complete-ayah recitations come from EveryAyah. Those providers may receive normal connection information such as an IP address and request metadata.'],
      ['Feature permissions', 'Recite & Understand requests microphone access only after you choose to recite. Tilawa processes that audio entirely on your device; Kalima does not store or upload it. Optional lock-screen lessons never bypass the keyguard, PIN, password, or biometrics. Everything remains off until you turn it on.'],
      ['Email, deletion, and retention', 'If you email support, your address and message are used to respond and troubleshoot. Delete local data by clearing Kalima’s app data or uninstalling it; exported backups must be deleted separately. Support correspondence is retained only as needed for the conversation, troubleshooting, legal obligations, or abuse prevention.'],
      ['Children and changes', 'Kalima does not knowingly request or collect personal information from children. A parent or guardian should supervise email or external-site use. Material policy changes will be published here with a new effective date.'],
    ],
  },
  pt: {
    eyebrow: 'Política de privacidade', title: 'Privacidade clara e humana.', intro: 'Vigente desde 28 de agosto de 2026. O Kalima é um app de árabe corânico que prioriza o uso offline, desenvolvido por Uthman (Gustavo).',
    sections: [
      ['O que fica no seu aparelho', 'O progresso e as preferências ficam armazenados localmente. Isso pode incluir palavras aprendidas ou em revisão, seleções de estudo, progresso inicial, lembretes, áudios baixados e preferências visuais. O desenvolvedor não recebe essas informações.'],
      ['Backups sob seu controle', 'Você pode exportar um backup do progresso para um local escolhido e importá-lo depois. O Kalima não envia esse arquivo. Você controla qualquer serviço externo ou aparelho para onde decida copiá-lo.'],
      ['Internet e áudio', 'As lições principais, os dados de estudo e o texto do Alcorão vêm incluídos para uso offline. Quando você escolhe ouvir ou baixar áudio, as palavras vêm do QuranCDN e as recitações de ayahs completas vêm do EveryAyah. Esses serviços podem receber informações normais da conexão, como endereço IP e metadados.'],
      ['Permissões opcionais', 'Recite e Entenda solicita o microfone somente quando você decide recitar. O Tilawa processa esse áudio inteiramente no aparelho; o Kalima não o armazena nem envia. As lições opcionais na tela bloqueada nunca contornam o bloqueio, PIN, senha ou biometria. Tudo permanece desligado até você ativar.'],
      ['E-mail, exclusão e retenção', 'Ao enviar e-mail ao suporte, seu endereço e mensagem são usados para responder e investigar o problema. Exclua os dados locais limpando os dados do app ou desinstalando-o; backups exportados devem ser apagados separadamente. A correspondência é mantida somente enquanto necessária.'],
      ['Crianças e alterações', 'O Kalima não solicita nem coleta intencionalmente informações pessoais de crianças. Um responsável deve supervisionar contatos opcionais por e-mail ou sites externos. Mudanças relevantes serão publicadas aqui com uma nova data.'],
    ],
  },
} as const;

function Privacy({ language }: { language: Language }) {
  const t = policy[language];
  return <article className="legal-page section-pad"><div className="legal-hero"><span className="section-kicker">{t.eyebrow}</span><h1>{t.title}</h1><p>{t.intro}</p><div className="policy-badges"><span>No ads</span><span>No accounts</span><span>No analytics</span></div></div>
    <div className="legal-grid">{t.sections.map(([title, body]) => <section key={title}><h2>{title}</h2><p>{body}</p></section>)}</div>
    <div className="contact-strip"><strong>{language === 'en' ? 'Questions or privacy requests?' : 'Dúvidas ou solicitações de privacidade?'}</strong><a href={`mailto:${facts.email}`}>{facts.email}</a></div>
  </article>;
}

function Support({ language }: { language: Language }) {
  const pt = language === 'pt';
  return <article className="support-page section-pad"><div className="support-hero"><span className="section-kicker">{pt ? 'Suporte' : 'Support'}</span><h1>{pt ? 'Uma pessoa de verdade está por trás do Kalima.' : 'A real person is behind Kalima.'}</h1><p>{pt ? 'O Kalima é desenvolvido de forma independente por Uthman (Gustavo). Para relatar um problema, inclua a versão instalada e o modelo do seu aparelho.' : 'Kalima is independently developed by Uthman (Gustavo). When reporting an issue, include the installed app version and your device model.'}</p><a className="button gold" href={`mailto:${facts.email}?subject=Kalima%20support`}>{pt ? 'Enviar e-mail' : 'Email support'} <span>→</span></a></div>
    <div className="support-grid">
      <div className="support-card"><span>01</span><h2>{pt ? 'Informações úteis' : 'Helpful details'}</h2><ul><li>{pt ? 'O que você estava tentando fazer' : 'What you were trying to do'}</li><li>{pt ? 'O que aconteceu' : 'What happened instead'}</li><li>{pt ? 'Modelo e versão do Android' : 'Device model and Android version'}</li><li>{pt ? 'Uma captura de tela, se possível' : 'A screenshot, if possible'}</li></ul></div>
      <div className="support-card"><span>02</span><h2>{pt ? 'Sobre o app' : 'About the app'}</h2><dl><div><dt>{pt ? 'Plataforma' : 'Platform'}</dt><dd>Android 8+</dd></div><div><dt>{pt ? 'Idiomas' : 'Languages'}</dt><dd>English · Português</dd></div><div><dt>{pt ? 'Preço' : 'Price'}</dt><dd>{pt ? 'Grátis, sem anúncios' : 'Free, no ads'}</dd></div></dl></div>
      <div className="support-card support-card-dark"><span>03</span><h2>{pt ? 'Apoie o desenvolvimento' : 'Support development'}</h2><p>{pt ? 'Se o Kalima ajuda você, uma contribuição apoia o desenvolvimento independente e futuros apps para muçulmanos.' : 'If Kalima helps you, a contribution supports independent development and future apps for Muslims.'}</p><a href={facts.support}>{pt ? 'Perguntar como apoiar' : 'Ask how to contribute'} →</a></div>
    </div>
  </article>;
}

function Footer({ language }: { language: Language }) {
  const t = content[language];
  return <footer className="site-footer section-pad"><div className="footer-top"><div className="brand footer-brand"><img src="/kalima-icon.png" alt="" /><span>Kalima</span></div><p>{t.footer}</p><div className="footer-links"><SmartLink href="/privacy">{t.nav.privacy}</SmartLink><SmartLink href="/support">{t.nav.support}</SmartLink><a href={`mailto:${facts.email}`}>Email</a></div></div><div className="footer-bottom"><span>© 2026 Uthman (Gustavo)</span><span>{t.legal}</span></div></footer>;
}

export default App;
