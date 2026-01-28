// src/pages/legal/PrivacyPolicyPage.tsx
import { ArrowRight, FileText, Lock, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function PrivacyPolicyPage() {
  return (
    <div className="bg-white">
      {/* HERO */}
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-14 md:py-18">
          <div className="text-sm text-neutral-500">
            <Link className="hover:text-neutral-900" to="/">
              Home
            </Link>
            <span className="mx-2">/</span>
            <span className="text-neutral-800 font-semibold">Política de Privacidade</span>
          </div>

          <div className="mt-8">
            <span className="inline-flex items-center rounded-full bg-orange-50 text-primary-700 border border-orange-100 px-3 py-1 text-xs font-extrabold tracking-wide">
              LGPD
            </span>

            <h1 className="mt-4 text-4xl md:text-5xl font-extrabold text-neutral-900 leading-tight">
              Política de Privacidade – PLANTAHUB
            </h1>

            <p className="mt-4 text-neutral-600 leading-relaxed max-w-3xl">
              O PlantaHUB respeita a sua privacidade e está comprometido com a proteção dos dados
              pessoais, em conformidade com a Lei Geral de Proteção de Dados (LGPD – Lei nº
              13.709/2018).
            </p>

            <div className="mt-6 flex flex-wrap items-center gap-6 text-xs font-semibold text-neutral-600">
              <span className="inline-flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary-500" />
                Segurança
              </span>
              <span className="inline-flex items-center gap-2">
                <Lock className="h-4 w-4 text-primary-500" />
                Privacidade
              </span>
              <span className="inline-flex items-center gap-2">
                <FileText className="h-4 w-4 text-primary-500" />
                Transparência
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* CONTENT */}
      <section className="bg-neutral-50">
        <div className="max-w-7xl mx-auto px-6 py-14">
          <div className="grid gap-8 lg:grid-cols-12">
            {/* left nav */}
            <aside className="lg:col-span-4">
              <div className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm sticky top-6">
                <div className="text-sm font-extrabold text-neutral-900">Nesta página</div>
                <nav className="mt-4 space-y-2 text-sm">
                  {[
                    'Dados Coletados',
                    'Finalidade do Tratamento',
                    'Compartilhamento de Dados',
                    'Cookies',
                    'Armazenamento e Segurança',
                    'Direitos do Titular',
                    'Retenção de Dados',
                    'Alterações nesta Política',
                    'Contato',
                  ].map(t => (
                    <a
                      key={t}
                      href={`#${slugify(t)}`}
                      className="block rounded-xl px-3 py-2 text-neutral-700 hover:bg-neutral-50 hover:text-neutral-900 transition"
                    >
                      {t}
                    </a>
                  ))}
                </nav>

                <div className="mt-6 rounded-xl border border-neutral-200 bg-neutral-50 p-4">
                  <div className="text-xs font-extrabold text-neutral-900">Última atualização</div>
                  <div className="mt-1 text-sm text-neutral-700">27/01/2026</div>
                </div>

                <div className="mt-4">
                  <Link
                    to="/legal/termos"
                    className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-primary-500 text-white font-semibold py-3 hover:bg-primary-600 transition"
                  >
                    Ver Termos de Serviço <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              </div>
            </aside>

            {/* main */}
            <main className="lg:col-span-8">
              <Article>
                <Section id={slugify('Dados Coletados')} title="1. Dados Coletados">
                  <p>Podemos coletar:</p>
                  <ul>
                    <li>Nome completo;</li>
                    <li>E-mail;</li>
                    <li>CPF/CNPJ (quando necessário para emissão fiscal);</li>
                    <li>Dados de navegação (IP, cookies, páginas acessadas);</li>
                    <li>Informações de pagamento (processadas por terceiros).</li>
                  </ul>
                </Section>

                <Section
                  id={slugify('Finalidade do Tratamento')}
                  title="2. Finalidade do Tratamento"
                >
                  <p>Os dados são utilizados para:</p>
                  <ul>
                    <li>Criar e gerenciar contas;</li>
                    <li>Processar compras e liberar downloads;</li>
                    <li>Emitir documentos fiscais;</li>
                    <li>Enviar comunicações transacionais;</li>
                    <li>Melhorar a experiência do usuário.</li>
                  </ul>
                </Section>

                <Section
                  id={slugify('Compartilhamento de Dados')}
                  title="3. Compartilhamento de Dados"
                >
                  <p>Seus dados poderão ser compartilhados apenas com:</p>
                  <ul>
                    <li>Gateways de pagamento;</li>
                    <li>Plataformas de hospedagem e infraestrutura;</li>
                    <li>Autoridades legais, quando exigido por lei.</li>
                  </ul>
                  <p className="mt-4 font-semibold text-neutral-900">
                    Nunca vendemos ou comercializamos dados pessoais.
                  </p>
                </Section>

                <Section id={slugify('Cookies')} title="4. Cookies">
                  <p>Utilizamos cookies para:</p>
                  <ul>
                    <li>Funcionalidade da plataforma;</li>
                    <li>Análise de desempenho;</li>
                    <li>Melhoria contínua da experiência do usuário.</li>
                  </ul>
                  <p className="mt-4">
                    O usuário pode gerenciar cookies nas configurações do navegador.
                  </p>
                </Section>

                <Section
                  id={slugify('Armazenamento e Segurança')}
                  title="5. Armazenamento e Segurança"
                >
                  <p>
                    Adotamos medidas técnicas e administrativas para proteger os dados contra
                    acessos não autorizados, vazamentos ou usos indevidos.
                  </p>
                </Section>

                <Section id={slugify('Direitos do Titular')} title="6. Direitos do Titular">
                  <p>Você pode, a qualquer momento:</p>
                  <ul>
                    <li>Solicitar acesso aos seus dados;</li>
                    <li>Corrigir informações;</li>
                    <li>Solicitar exclusão ou anonimização;</li>
                    <li>Revogar consentimentos.</li>
                  </ul>
                  <p className="mt-4">
                    As solicitações podem ser feitas pelo canal oficial de contato do PlantaHUB.
                  </p>
                </Section>

                <Section id={slugify('Retenção de Dados')} title="7. Retenção de Dados">
                  <p>
                    Os dados serão armazenados apenas pelo tempo necessário para cumprir as
                    finalidades legais, contratuais e regulatórias.
                  </p>
                </Section>

                <Section
                  id={slugify('Alterações nesta Política')}
                  title="8. Alterações nesta Política"
                >
                  <p>
                    Esta Política pode ser atualizada a qualquer momento. Recomendamos a consulta
                    periódica.
                  </p>
                </Section>

                <Section id={slugify('Contato')} title="9. Contato">
                  <p>
                    Para dúvidas, solicitações ou reclamações sobre privacidade, entre em contato
                    pelos canais oficiais do PlantaHUB.
                  </p>
                </Section>
              </Article>
            </main>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="bg-primary-500">
        <div className="max-w-7xl mx-auto px-6 py-14 text-center">
          <h2 className="text-3xl font-extrabold text-white">Precisa falar com a gente?</h2>
          <p className="mt-2 text-white/90">
            Para solicitações de privacidade, dúvidas ou suporte, estamos disponíveis.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to="/contato"
              className="rounded-xl bg-white text-neutral-900 font-semibold px-6 py-3 hover:bg-neutral-100 transition inline-flex items-center gap-2"
            >
              Ir para Contato <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              to="/produtos"
              className="rounded-xl border border-white/40 text-white font-semibold px-6 py-3 hover:bg-white/10 transition"
            >
              Ver catálogo
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

/* ---------- UI helpers ---------- */

function Article({ children }: { children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-6 md:p-8 shadow-sm">
      <div className="prose prose-neutral max-w-none prose-p:text-neutral-700 prose-li:text-neutral-700 prose-strong:text-neutral-900">
        {children}
      </div>
    </div>
  );
}

function Section({
  id,
  title,
  children,
}: {
  id: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section id={id} className="scroll-mt-24">
      <h2 className="text-xl font-extrabold text-neutral-900">{title}</h2>
      <div className="mt-3 text-neutral-700 leading-relaxed">{children}</div>
      <div className="mt-8 border-b border-neutral-200" />
    </section>
  );
}

function slugify(s: string) {
  return s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
}
