import { useEffect, useState, type ReactNode } from 'react';

type Language = 'en' | 'pt';
type Route = '/' | '/privacy' | '/support';

const facts = {
  email: 'uthman-al-brazili@proton.me',
  support: 'mailto:uthman-al-brazili@proton.me?subject=Supporting%20Kalima',
};

const content = {
  en: {
    nav: { learn: 'How it works', features: 'Features', privacy: 'Privacy', support: 'Support', cta: 'Get Kalima' },
    hero: {
      eyebrow: 'Quranic Arabic • Android',
      title: 'Quranic Arabic, one meaningful word at a time.',
      body: 'Build a useful vocabulary with short lessons, real Quran audio, and a learning path that meets you where you are.',
      cta: 'Get Kalima',
      secondary: 'See how it works',
      note: 'Free • No ads • Android 8+',
    },
    card: { label: 'A word from the Quran', transliteration: 'hudā', meaning: 'guidance', again: 'Again', gotIt: 'Got it', ready: 'Choose how this word felt.', saved: 'Nice. Kalima will bring it back at the right time.', retry: 'No pressure. This word will return sooner.' },
    proof: [
      ['42,117', 'offline study cards'],
      ['114', 'surahs to read'],
      ['0', 'ads or trackers'],
    ],
    promise: 'A calm path from first letters to Quranic context.',
    promiseBody: 'Kalima combines clear foundations, focused recall, and the full Arabic Quran in one private learning space.',
    steps: [
      ['01', 'Start at your level', 'Choose whether you already know the Arabic alphabet and numbers. Kalima shapes the foundation path around your answer.'],
      ['02', 'Learn in context', 'Study Arabic text, transliteration, meaning, grammar, root, and the Quran reference together.'],
      ['03', 'Return at the right time', 'Spaced repetition brings words back when they are due—without streak anxiety or noisy rewards.'],
    ],
    screensTitle: 'The learning experience, not a marketing mockup.',
    screensBody: 'Real screens from Kalima show a guided start, focused vocabulary practice, varied quizzes, and an offline Quran reader.',
    screens: [
      ['foundations.png', 'Arabic foundations at hand', 'Review the alphabet, vowel forms, joining, and numbers whenever you need them.'],
      ['study.png', 'One clear study card', 'See the word, listen, and explore its Quranic context.'],
      ['quiz.png', 'Practice that changes shape', 'Mixed, listening, cloze, and root questions keep recall active.'],
      ['quran.png', 'Read all 114 surahs', 'The full Arabic Quran is included for offline reading.'],
    ],
    featuresTitle: 'Serious learning can still feel inviting.',
    features: [
      ['↻', 'Spaced repetition', 'Review what is due instead of restarting from the beginning.'],
      ['◖', 'Real Quran audio', 'Hear word recordings and Al-Hussary’s complete-ayah recitation. Played audio can be saved locally.'],
      ['◇', 'Flexible study sets', 'Choose essential words, prayer vocabulary, short surahs, specific surahs, or the complete corpus.'],
      ['☼', 'Gentle routine tools', 'Optional reminders and return-to-phone cards help practice fit naturally into your day.'],
      ['أ', 'Foundations included', 'Learn all 28 Arabic letters, joining, short vowels, and Arabic-Indic digits from ٠ to ٩.'],
      ['⌁', 'Context, not isolation', 'Open the ayah, inspect roots and grammar, and find other indexed occurrences offline.'],
    ],
    trustTitle: 'Your learning belongs to you.',
    trustBody: 'No account, advertising, analytics, or remote learning database. Progress stays on your device and can be exported manually.',
    trustLinks: ['Read the privacy policy', 'See support details'],
    availability: { kicker: 'Kalima for Android', title: 'Learn with clarity. Continue with purpose.', body: 'Kalima is free, bilingual, and available as a universal Android app.', cta: 'Release information', secondary: 'Contact support' },
    footer: 'Independent, thoughtful software for Quranic Arabic learners.',
    legal: 'Kalima is a learning aid and does not replace a scholarly translation or qualified religious instruction.',
  },
  pt: {
    nav: { learn: 'Como funciona', features: 'Recursos', privacy: 'Privacidade', support: 'Suporte', cta: 'Conheça o Kalima' },
    hero: {
      eyebrow: 'Árabe corânico • Android',
      title: 'Árabe corânico, uma palavra significativa de cada vez.',
      body: 'Construa um vocabulário útil com lições curtas, áudio real do Alcorão e um caminho que começa no seu nível.',
      cta: 'Conheça o Kalima',
      secondary: 'Veja como funciona',
      note: 'Grátis • Sem anúncios • Android 8+',
    },
    card: { label: 'Uma palavra do Alcorão', transliteration: 'hudā', meaning: 'orientação', again: 'De novo', gotIt: 'Entendi', ready: 'Como esta palavra pareceu?', saved: 'Muito bem. O Kalima vai trazê-la de volta no momento certo.', retry: 'Sem pressão. Esta palavra voltará mais cedo.' },
    proof: [
      ['42.117', 'cartões offline'],
      ['114', 'suras para ler'],
      ['0', 'anúncios ou rastreadores'],
    ],
    promise: 'Um caminho tranquilo das primeiras letras ao contexto do Alcorão.',
    promiseBody: 'O Kalima reúne fundamentos claros, memorização focada e o Alcorão completo em árabe em um só espaço privado.',
    steps: [
      ['01', 'Comece no seu nível', 'Diga se você já conhece o alfabeto árabe e os números. O Kalima adapta o caminho inicial à sua resposta.'],
      ['02', 'Aprenda no contexto', 'Estude o texto árabe, transliteração, significado, gramática, raiz e referência juntos.'],
      ['03', 'Revise no momento certo', 'A repetição espaçada traz cada palavra de volta quando ela precisa ser revisada, sem ansiedade por sequências.'],
    ],
    screensTitle: 'A experiência real, não uma tela de marketing.',
    screensBody: 'Telas do próprio Kalima mostram o início guiado, o estudo focado, os quizzes variados e o leitor offline do Alcorão.',
    screens: [
      ['foundations.png', 'Fundamentos sempre à mão', 'Revise o alfabeto, as vogais, as ligações e os números quando precisar.'],
      ['study.png', 'Um cartão claro', 'Veja a palavra, ouça e explore seu contexto no Alcorão.'],
      ['quiz.png', 'Prática variada', 'Questões mistas, de áudio, lacunas e raízes mantêm a memória ativa.'],
      ['quran.png', 'Leia as 114 suras', 'O Alcorão completo em árabe está incluído para leitura offline.'],
    ],
    featuresTitle: 'Aprendizado sério também pode ser acolhedor.',
    features: [
      ['↻', 'Repetição espaçada', 'Revise o que está pendente em vez de sempre recomeçar do início.'],
      ['◖', 'Áudio real do Alcorão', 'Ouça palavras e a recitação de ayahs completas por Al-Hussary. Os áudios tocados podem ser salvos.'],
      ['◇', 'Conjuntos flexíveis', 'Escolha palavras essenciais, vocabulário da oração, suras curtas, suras específicas ou o corpus completo.'],
      ['☼', 'Rotina sem pressão', 'Lembretes e cartões opcionais ao voltar ao celular ajudam o estudo a caber no seu dia.'],
      ['أ', 'Fundamentos incluídos', 'Aprenda as 28 letras, ligações, vogais breves e os algarismos arábico-indianos de ٠ a ٩.'],
      ['⌁', 'Palavras em contexto', 'Abra o ayah, consulte raiz e gramática e encontre outras ocorrências offline.'],
    ],
    trustTitle: 'Seu aprendizado pertence a você.',
    trustBody: 'Sem conta, publicidade, análises ou banco remoto de atividades. O progresso fica no aparelho e pode ser exportado manualmente.',
    trustLinks: ['Leia a política de privacidade', 'Veja os detalhes de suporte'],
    availability: { kicker: 'Kalima para Android', title: 'Aprenda com clareza. Continue com propósito.', body: 'O Kalima é gratuito, bilíngue e distribuído como app Android universal.', cta: 'Informações do app', secondary: 'Falar com o suporte' },
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
  return (
    <div className="study-stage" aria-label="Interactive study card preview">
      <span className="orbit-dot dot-one" />
      <span className="orbit-dot dot-two" />
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
    </div>
  );
}

function Home({ language }: { language: Language }) {
  const t = content[language];
  const screenshotLocale = language === 'pt' ? 'pt-BR' : 'en';
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

    <section className="path-section section-pad" id="how">
      <div className="section-heading centered">
        <span className="section-kicker">{language === 'en' ? 'A path with purpose' : 'Um caminho com propósito'}</span>
        <h2>{t.promise}</h2>
        <p>{t.promiseBody}</p>
      </div>
      <div className="step-grid">
        {t.steps.map(([number, title, body], index) => <article className={`step-card step-${index + 1}`} key={number}>
          <span className="step-number">{number}</span>
          <div className="step-icon" aria-hidden="true">{['أ', 'هُدًى', '↻'][index]}</div>
          <h3>{title}</h3><p>{body}</p>
        </article>)}
      </div>
    </section>

    <section className="screens-section" aria-labelledby="screens-title">
      <div className="section-pad screens-intro">
        <div className="section-heading"><span className="section-kicker">{language === 'en' ? 'Inside the app' : 'Por dentro do app'}</span><h2 id="screens-title">{t.screensTitle}</h2></div>
        <p>{t.screensBody}</p>
      </div>
      <div className="screen-rail section-pad">
        {t.screens.map(([image, title, body], index) => <article className={`screen-card screen-tone-${index + 1}`} key={image}>
          <div className="phone-frame"><img src={`/screens/${screenshotLocale}/${image}`} alt={`${title} — ${language === 'pt' ? 'Tela do Kalima para Android' : 'Kalima Android screen'}`} loading="lazy" /></div>
          <div className="screen-copy"><span>0{index + 1}</span><h3>{title}</h3><p>{body}</p></div>
        </article>)}
      </div>
    </section>

    <section className="features-section section-pad" id="features">
      <div className="section-heading centered"><span className="section-kicker">{language === 'en' ? 'Built for real study' : 'Feito para estudar de verdade'}</span><h2>{t.featuresTitle}</h2></div>
      <div className="feature-grid">
        {t.features.map(([icon, title, body]) => <article className="feature-card" key={title}><span className="feature-icon" aria-hidden="true">{icon}</span><h3>{title}</h3><p>{body}</p></article>)}
      </div>
    </section>

    <section className="trust-wrap section-pad">
      <div className="trust-panel">
        <div className="privacy-seal" aria-hidden="true"><span>✓</span><small>PRIVATE<br />BY DESIGN</small></div>
        <div><span className="section-kicker light">{language === 'en' ? 'Privacy without fine print' : 'Privacidade sem letras miúdas'}</span><h2>{t.trustTitle}</h2><p>{t.trustBody}</p>
          <div className="inline-links"><SmartLink href="/privacy">{t.trustLinks[0]} →</SmartLink><SmartLink href="/support">{t.trustLinks[1]} →</SmartLink></div>
        </div>
      </div>
    </section>

    <section className="availability section-pad" id="availability">
      <div className="availability-card">
        <div><span className="section-kicker">{t.availability.kicker}</span><h2>{t.availability.title}</h2><p>{t.availability.body}</p>
          <div className="hero-actions"><SmartLink className="button dark" href="/support">{t.availability.cta}</SmartLink><a className="button ghost" href={`mailto:${facts.email}`}>{t.availability.secondary}</a></div>
        </div>
        <img src="/kalima-icon.png" alt="Kalima app icon" />
      </div>
    </section>
  </>;
}

const policy = {
  en: {
    eyebrow: 'Privacy policy', title: 'Clear, human privacy.', intro: 'Effective August 20, 2026. Kalima is an offline-first Quranic Arabic learning app developed by Uthman (Gustavo).',
    sections: [
      ['What stays on your device', 'Study progress and preferences are stored locally. This may include learned and reviewing words, study selections, foundation progress, reminders, downloaded audio, and display preferences. The developer does not receive this information.'],
      ['Backups you control', 'You may export a progress backup to a file location you choose and import it later. Kalima does not upload that backup. You control any external service or device location where you copy it.'],
      ['Network access and audio', 'Core lessons, study data, and Quran text are bundled for offline use. When you choose to stream or download Quran audio, word recordings come from QuranCDN and complete-ayah recitations come from EveryAyah. Those providers may receive normal connection information such as an IP address and request metadata.'],
      ['Optional permissions', 'Notifications are used only for reminders you enable. Display-over-other-apps and foreground-service access support the optional return-to-phone study card. Boot access restores features you previously enabled after a restart. These features remain off until you turn them on.'],
      ['Email, deletion, and retention', 'If you email support, your address and message are used to respond and troubleshoot. Delete local data by clearing Kalima’s app data or uninstalling it; exported backups must be deleted separately. Support correspondence is retained only as needed for the conversation, troubleshooting, legal obligations, or abuse prevention.'],
      ['Children and changes', 'Kalima does not knowingly request or collect personal information from children. A parent or guardian should supervise optional email or external-site use. Material policy changes will be published here with a new effective date.'],
    ],
  },
  pt: {
    eyebrow: 'Política de privacidade', title: 'Privacidade clara e humana.', intro: 'Vigente desde 20 de agosto de 2026. O Kalima é um app de árabe corânico que prioriza o uso offline, desenvolvido por Uthman (Gustavo).',
    sections: [
      ['O que fica no seu aparelho', 'O progresso e as preferências ficam armazenados localmente. Isso pode incluir palavras aprendidas ou em revisão, seleções de estudo, progresso inicial, lembretes, áudios baixados e preferências visuais. O desenvolvedor não recebe essas informações.'],
      ['Backups sob seu controle', 'Você pode exportar um backup do progresso para um local escolhido e importá-lo depois. O Kalima não envia esse arquivo. Você controla qualquer serviço externo ou aparelho para onde decida copiá-lo.'],
      ['Internet e áudio', 'As lições principais, os dados de estudo e o texto do Alcorão vêm incluídos para uso offline. Quando você escolhe ouvir ou baixar áudio, as palavras vêm do QuranCDN e as recitações de ayahs completas vêm do EveryAyah. Esses serviços podem receber informações normais da conexão, como endereço IP e metadados.'],
      ['Permissões opcionais', 'Notificações são usadas apenas para lembretes ativados por você. Exibição sobre outros apps e serviço em primeiro plano permitem o cartão opcional ao voltar ao celular. O acesso após a inicialização restaura recursos antes ativados. Tudo permanece desligado até você ativar.'],
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
      <div className="support-card support-card-dark"><span>03</span><h2>{pt ? 'Apoie o desenvolvimento' : 'Support development'}</h2><p>{pt ? 'Se o Kalima ajuda você, uma contribuição opcional apoia o desenvolvimento independente e futuros apps para muçulmanos.' : 'If Kalima helps you, an optional contribution supports independent development and future apps for Muslims.'}</p><a href={facts.support}>{pt ? 'Perguntar como apoiar' : 'Ask how to contribute'} →</a></div>
    </div>
  </article>;
}

function Footer({ language }: { language: Language }) {
  const t = content[language];
  return <footer className="site-footer section-pad"><div className="footer-top"><div className="brand footer-brand"><img src="/kalima-icon.png" alt="" /><span>Kalima</span></div><p>{t.footer}</p><div className="footer-links"><SmartLink href="/privacy">{t.nav.privacy}</SmartLink><SmartLink href="/support">{t.nav.support}</SmartLink><a href={`mailto:${facts.email}`}>Email</a></div></div><div className="footer-bottom"><span>© 2026 Uthman (Gustavo)</span><span>{t.legal}</span></div></footer>;
}

export default App;
