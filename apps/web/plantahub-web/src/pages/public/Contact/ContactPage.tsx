// src/pages/ContactPage.tsx
import {
  ArrowRight,
  BadgeCheck,
  Clock3,
  Facebook,
  Headset,
  Instagram,
  Linkedin,
  Mail,
  MapPin,
  MessageCircle,
  MessageSquare,
  Phone,
  Send,
  ShieldCheck,
  Youtube,
} from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';

type FormState = {
  name: string;
  email: string;
  subject: string;
  message: string;
};

export default function ContactPage() {
  const [form, setForm] = useState<FormState>({
    name: '',
    email: '',
    subject: '',
    message: '',
  });

  const isValid = useMemo(() => {
    return (
      form.name.trim().length >= 2 &&
      form.email.trim().includes('@') &&
      form.subject.trim().length >= 3 &&
      form.message.trim().length >= 10
    );
  }, [form]);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm(prev => ({ ...prev, [key]: value }));
  }

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!isValid) return;

    // TODO: integrar backend / email service
    console.log('Contact form submit', form);

    // feedback simples (pode trocar por toast)
    alert('Mensagem enviada! Em breve retornaremos seu contato.');

    setForm({ name: '', email: '', subject: '', message: '' });
  }

  return (
    <div className="bg-white">
      {/* HERO */}
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
          <div className="mt-8 grid gap-10 lg:grid-cols-2 lg:items-center">
            <div>
              <span className="inline-flex items-center rounded-full bg-orange-50 text-primary-700 border border-orange-100 px-3 py-1 text-xs font-extrabold tracking-wide">
                FALE COM A PLANTAHUB
              </span>

              <h1 className="mt-4 text-4xl md:text-5xl font-extrabold text-neutral-900 leading-tight">
                Vamos conversar sobre seu projeto
              </h1>

              <p className="mt-4 text-neutral-600 leading-relaxed max-w-xl">
                Seja para dúvidas sobre compras, suporte técnico, parcerias ou personalizações,
                nossa equipe está pronta para ajudar com rapidez e clareza.
              </p>

              {/* trust row */}
              <div className="mt-6 flex flex-wrap items-center gap-6 text-xs font-semibold text-neutral-600">
                <span className="inline-flex items-center gap-2">
                  <ShieldCheck className="h-4 w-4 text-primary-500" />
                  Pagamento seguro
                </span>
                <span className="inline-flex items-center gap-2">
                  <Headset className="h-4 w-4 text-primary-500" />
                  Suporte profissional
                </span>
                <span className="inline-flex items-center gap-2">
                  <BadgeCheck className="h-4 w-4 text-primary-500" />
                  Padrão CAU/CREA
                </span>
              </div>
            </div>

            {/* “image card” pattern (placeholder) */}
            <div className="lg:justify-self-end">
              <div className="relative">
                <div className="absolute -top-4 -right-4 h-14 w-14 rounded-xl border-2 border-primary-500" />
                <div className="absolute -bottom-5 -left-6 h-20 w-20 rounded-2xl bg-orange-100/70" />

                <div className="rounded-2xl border border-neutral-200 overflow-hidden shadow-sm bg-neutral-50">
                  <img
                    src="/brand/logotipo.png"
                    alt="Contato PlantaHUB"
                    className="w-full h-80 md:h-80 object-cover"
                    loading="lazy"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CONTACT GRID */}
      <section className="bg-neutral-50">
        <div className="max-w-7xl mx-auto px-6 py-16">
          <div className="grid gap-6 lg:grid-cols-3">
            {/* LEFT — contact cards */}
            <div className="space-y-6">
              <InfoCard
                icon={<Mail className="h-5 w-5 text-primary-600" />}
                title="Email"
                text="Envie sua mensagem e retornaremos o mais rápido possível."
                line="contato@plantahub.com.br"
              />
              <InfoCard
                icon={<Phone className="h-5 w-5 text-primary-600" />}
                title="Telefone / WhatsApp"
                text="Atendimento para suporte e informações gerais."
                line="(xx) xxxxx-xxxx"
              />
              <InfoCard
                icon={<MapPin className="h-5 w-5 text-primary-600" />}
                title="Localização"
                text="Atendimento remoto com suporte para todo o Brasil."
                line="Santa Rita do Sapucaí — MG"
              />
              <InfoCard
                icon={<Clock3 className="h-5 w-5 text-primary-600" />}
                title="Horário"
                text="Segunda a sexta"
                line="09:00 — 18:00"
              />

              <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                <h3 className="text-lg font-extrabold text-neutral-900">
                  Conecte-se com a PLANTAHUB
                </h3>

                <p className="mt-2 text-sm text-neutral-600">
                  Acompanhe novidades, lançamentos de plantas, conteúdos técnicos e bastidores do
                  nosso trabalho.
                </p>

                <div className="mt-5 grid grid-cols-2 gap-3">
                  <SocialLink
                    href="https://instagram.com/plantahub"
                    icon={<Instagram className="h-4 w-4" />}
                    label="Instagram"
                  />

                  <SocialLink
                    href="https://linkedin.com/company/plantahub"
                    icon={<Linkedin className="h-4 w-4" />}
                    label="LinkedIn"
                  />

                  <SocialLink
                    href="https://wa.me/5500000000000"
                    icon={<MessageCircle className="h-4 w-4" />}
                    label="WhatsApp"
                  />

                  <SocialLink
                    href="https://facebook.com/plantahub"
                    icon={<Facebook className="h-4 w-4" />}
                    label="Facebook"
                  />

                  <SocialLink
                    href="https://youtube.com/@plantahub"
                    icon={<Youtube className="h-4 w-4" />}
                    label="YouTube"
                  />
                </div>
              </div>

              <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                <div className="flex items-start gap-3">
                  <div className="h-10 w-10 rounded-xl bg-orange-50 border border-orange-100 flex items-center justify-center">
                    <MessageSquare className="h-5 w-5 text-primary-600" />
                  </div>
                  <div>
                    <div className="font-extrabold text-neutral-900">
                      Precisa de suporte pós-compra?
                    </div>
                    <p className="mt-1 text-sm text-neutral-600 leading-relaxed">
                      Se você já comprou uma planta e precisa de ajuda, envie o ID do pedido e uma
                      breve descrição.
                    </p>
                  </div>
                </div>

                <div className="mt-4">
                  <button className="w-full rounded-xl border border-neutral-300 bg-white font-semibold cursor-pointer text-neutral-900 hover:bg-neutral-100 transition inline-flex items-center justify-center gap-2 py-3">
                    Central de Ajuda <ArrowRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>

            {/* RIGHT — form */}
            <div className="lg:col-span-2">
              <div className="rounded-2xl border border-neutral-200 bg-white p-6 md:p-8 shadow-sm">
                <h2 className="text-2xl font-extrabold text-neutral-900">Envie uma mensagem</h2>
                <p className="mt-2 text-neutral-600">
                  Preencha os campos abaixo. Respondemos em até 24 horas úteis.
                </p>

                <form className="mt-8 space-y-5" onSubmit={onSubmit}>
                  <div className="grid gap-4 md:grid-cols-2">
                    <Field label="Seu nome">
                      <input
                        value={form.name}
                        onChange={e => set('name', e.target.value)}
                        className="w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-400"
                        placeholder="Ex.: João Silva"
                        autoComplete="name"
                      />
                    </Field>

                    <Field label="Seu email">
                      <input
                        value={form.email}
                        onChange={e => set('email', e.target.value)}
                        className="w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-400"
                        placeholder="voce@exemplo.com"
                        autoComplete="email"
                      />
                    </Field>
                  </div>

                  <Field label="Assunto">
                    <input
                      value={form.subject}
                      onChange={e => set('subject', e.target.value)}
                      className="w-full rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-400"
                      placeholder="Ex.: Dúvida sobre personalização"
                    />
                  </Field>

                  <Field label="Mensagem">
                    <textarea
                      value={form.message}
                      onChange={e => set('message', e.target.value)}
                      className="w-full min-h-35 rounded-xl border border-neutral-300 bg-white px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-400"
                      placeholder="Conte rapidamente o que você precisa e, se possível, inclua detalhes do seu projeto."
                    />
                  </Field>

                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pt-2">
                    <div className="text-xs text-neutral-500">
                      Ao enviar, você concorda com nosso tratamento básico de dados para retorno de
                      contato.
                    </div>

                    <button
                      type="submit"
                      disabled={!isValid}
                      className={[
                        'rounded-xl px-6 py-3 font-semibold inline-flex items-center justify-center gap-2 transition',
                        isValid
                          ? 'bg-primary-500 text-white hover:bg-primary-600'
                          : 'bg-neutral-200 text-neutral-500 cursor-not-allowed',
                      ].join(' ')}
                    >
                      <Send className="h-4 w-4" />
                      Enviar mensagem
                    </button>
                  </div>
                </form>
              </div>

              {/* map placeholder */}
              <div className="mt-6 rounded-2xl border border-neutral-200 bg-white overflow-hidden shadow-sm">
                <div className="p-6 md:p-8">
                  <h3 className="text-lg font-extrabold text-neutral-900">Atendimento remoto</h3>
                  <p className="mt-2 text-sm text-neutral-600">
                    A PLANTAHUB atende todo o Brasil. Se você precisar de uma chamada rápida,
                    podemos agendar um horário.
                  </p>

                  <div className="mt-5 grid gap-4 md:grid-cols-3">
                    <MiniPill
                      icon={<Headset className="h-4 w-4 text-primary-600" />}
                      text="Suporte técnico"
                    />
                    <MiniPill
                      icon={<ShieldCheck className="h-4 w-4 text-primary-600" />}
                      text="Compra e acesso"
                    />
                    <MiniPill
                      icon={<BadgeCheck className="h-4 w-4 text-primary-600" />}
                      text="CAU/CREA"
                    />
                  </div>
                </div>

                <div className="h-56 bg-neutral-100 border-t border-neutral-200 flex items-center justify-center text-neutral-500 text-sm">
                  {/* Você pode substituir por um iframe do Google Maps */}
                  Área para mapa / imagem (opcional)
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA footer (same pattern as other pages) */}
      <section className="bg-primary-500">
        <div className="max-w-7xl mx-auto px-6 py-16 text-center">
          <h2 className="text-3xl font-extrabold text-white">
            Quer explorar nossas plantas agora?
          </h2>
          <p className="mt-2 text-white/90">
            Veja projetos Confort, Prime e Diamond — prontos para download.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to="/casas"
              className="rounded-xl bg-white text-neutral-900 font-semibold px-6 py-3 hover:bg-neutral-100 transition inline-flex items-center gap-2"
            >
              Explorar Plantas <ArrowRight className="h-4 w-4" />
            </Link>

            <Link
              to="/produtos"
              className="rounded-xl border border-white/40 text-white font-semibold px-6 py-3 hover:bg-white/10 transition"
            >
              Ver catálogo completo
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

/* ---------- small UI components ---------- */

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="text-sm font-semibold text-neutral-900">{label}</span>
      <div className="mt-2">{children}</div>
    </label>
  );
}

function InfoCard({
  icon,
  title,
  text,
  line,
}: {
  icon: React.ReactNode;
  title: string;
  text: string;
  line: string;
}) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="h-10 w-10 rounded-xl bg-orange-50 border border-orange-100 flex items-center justify-center">
          {icon}
        </div>
        <div>
          <div className="font-extrabold text-neutral-900">{title}</div>
          <p className="mt-1 text-sm text-neutral-600 leading-relaxed">{text}</p>
          <div className="mt-3 text-sm font-semibold text-neutral-900">{line}</div>
        </div>
      </div>
    </div>
  );
}

function MiniPill({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="rounded-xl border border-neutral-200 bg-white px-4 py-3 text-sm font-semibold text-neutral-800 inline-flex items-center gap-2">
      <span className="h-7 w-7 rounded-lg bg-orange-50 border border-orange-100 flex items-center justify-center">
        {icon}
      </span>
      {text}
    </div>
  );
}

function SocialLink({ href, icon, label }: { href: string; icon: React.ReactNode; label: string }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="flex items-center gap-3 rounded-xl border border-neutral-200 bg-white px-4 py-3 text-sm font-semibold text-neutral-800 hover:bg-neutral-50 hover:border-primary-300 transition"
    >
      <span className="h-8 w-8 rounded-lg bg-orange-50 border border-orange-100 flex items-center justify-center text-primary-600">
        {icon}
      </span>
      {label}
    </a>
  );
}
