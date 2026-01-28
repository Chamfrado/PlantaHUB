// src/pages/legal/TermsOfServicePage.tsx
import { ArrowRight, BadgeCheck, FileText, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function TermsOfServicePage() {
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
            <span className="text-neutral-800 font-semibold">Termos de Serviço</span>
          </div>

          <div className="mt-8">
            <span className="inline-flex items-center rounded-full bg-orange-50 text-primary-700 border border-orange-100 px-3 py-1 text-xs font-extrabold tracking-wide">
              DOCUMENTO LEGAL
            </span>

            <h1 className="mt-4 text-4xl md:text-5xl font-extrabold text-neutral-900 leading-tight">
              Termos de Serviço – PLANTAHUB
            </h1>

            <p className="mt-4 text-neutral-600 leading-relaxed max-w-3xl">
              Ao acessar ou utilizar nossa plataforma, você concorda integralmente com estes Termos
              de Serviço. Caso não concorde com qualquer condição aqui descrita, recomendamos que
              não utilize nossos serviços.
            </p>

            <div className="mt-6 flex flex-wrap items-center gap-6 text-xs font-semibold text-neutral-600">
              <span className="inline-flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary-500" />
                Plataforma segura
              </span>
              <span className="inline-flex items-center gap-2">
                <BadgeCheck className="h-4 w-4 text-primary-500" />
                Conformidade legal
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
                    'Sobre o PlantaHUB',
                    'Elegibilidade',
                    'Cadastro e Conta',
                    'Produtos Digitais',
                    'Licença de Uso',
                    'Personalização de Projetos',
                    'Pagamentos',
                    'Política de Reembolso',
                    'Propriedade Intelectual',
                    'Limitação de Responsabilidade',
                    'Modificações',
                    'Foro',
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
                    to="/legal/privacidade"
                    className="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-primary-500 text-white font-semibold py-3 hover:bg-primary-600 transition"
                  >
                    Ver Política de Privacidade <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              </div>
            </aside>

            {/* main */}
            <main className="lg:col-span-8">
              <Article>
                <Section id={slugify('Sobre o PlantaHUB')} title="1. Sobre o PlantaHUB">
                  <p>
                    O PlantaHUB é uma plataforma digital que comercializa projetos arquitetônicos e
                    complementares em formato digital (BIM, DWG, PDF), destinados a uso técnico e
                    profissional, conforme descrito em cada produto.
                  </p>
                </Section>

                <Section id={slugify('Elegibilidade')} title="2. Elegibilidade">
                  <p>Para utilizar a plataforma, o usuário declara:</p>
                  <ul>
                    <li>Ter pelo menos 18 anos ou capacidade legal;</li>
                    <li>Fornecer informações verdadeiras, completas e atualizadas;</li>
                    <li>Utilizar os conteúdos de forma lícita.</li>
                  </ul>
                </Section>

                <Section id={slugify('Cadastro e Conta')} title="3. Cadastro e Conta">
                  <ul>
                    <li>O usuário é responsável pela confidencialidade de suas credenciais;</li>
                    <li>
                      O PlantaHUB não se responsabiliza por acessos indevidos causados por
                      negligência do usuário;
                    </li>
                    <li>
                      Reservamo-nos o direito de suspender ou encerrar contas em caso de violação
                      destes termos.
                    </li>
                  </ul>
                </Section>

                <Section id={slugify('Produtos Digitais')} title="4. Produtos Digitais">
                  <ul>
                    <li>
                      Todos os produtos são digitais e disponibilizados para download imediato,
                      conforme descrito na página do produto;
                    </li>
                    <li>Não há envio físico de materiais;</li>
                    <li>
                      Os arquivos disponibilizados seguem as especificações técnicas informadas.
                    </li>
                  </ul>
                </Section>

                <Section id={slugify('Licença de Uso')} title="5. Licença de Uso">
                  <p>
                    Ao adquirir um produto no PlantaHUB, o usuário recebe uma licença de uso não
                    exclusiva, intransferível e limitada, destinada a:
                  </p>
                  <ul>
                    <li>Uso pessoal ou profissional;</li>
                    <li>Execução de obra ou estudo técnico.</li>
                  </ul>
                  <p className="mt-4 font-semibold text-neutral-900">É expressamente proibido:</p>
                  <ul>
                    <li>Revender, sublicenciar ou redistribuir os arquivos;</li>
                    <li>Disponibilizar os projetos em plataformas públicas ou privadas;</li>
                    <li>Alterar os arquivos para fins de comercialização.</li>
                  </ul>
                </Section>

                <Section
                  id={slugify('Personalização de Projetos')}
                  title="6. Personalização de Projetos"
                >
                  <p>Quando oferecida, a personalização:</p>
                  <ul>
                    <li>Possui escopo limitado;</li>
                    <li>Não inclui novo projeto autoral completo, salvo contratação específica;</li>
                    <li>Pode ter prazos e condições próprias.</li>
                  </ul>
                </Section>

                <Section id={slugify('Pagamentos')} title="7. Pagamentos">
                  <ul>
                    <li>Os pagamentos são processados por gateways externos;</li>
                    <li>O acesso ao download ocorre após a confirmação do pagamento;</li>
                    <li>O PlantaHUB não armazena dados bancários ou de cartão.</li>
                  </ul>
                </Section>

                <Section id={slugify('Política de Reembolso')} title="8. Política de Reembolso">
                  <p>
                    Por se tratar de conteúdo digital com acesso imediato, não realizamos reembolsos
                    após o download, conforme o art. 49 do Código de Defesa do Consumidor, salvo
                    exceções legais.
                  </p>
                </Section>

                <Section id={slugify('Propriedade Intelectual')} title="9. Propriedade Intelectual">
                  <p>
                    Todos os conteúdos, marcas, layouts, textos, imagens e projetos são de
                    propriedade do PlantaHUB ou de seus licenciantes, protegidos pela legislação
                    vigente.
                  </p>
                </Section>

                <Section
                  id={slugify('Limitação de Responsabilidade')}
                  title="10. Limitação de Responsabilidade"
                >
                  <p>O PlantaHUB não se responsabiliza por:</p>
                  <ul>
                    <li>Uso inadequado dos projetos;</li>
                    <li>Execução da obra sem acompanhamento técnico profissional;</li>
                    <li>Adequações legais exigidas por legislações municipais ou estaduais.</li>
                  </ul>
                </Section>

                <Section id={slugify('Modificações')} title="11. Modificações">
                  <p>
                    O PlantaHUB pode atualizar estes Termos a qualquer momento. Recomenda-se a
                    revisão periódica.
                  </p>
                </Section>

                <Section id={slugify('Foro')} title="12. Foro">
                  <p>
                    Fica eleito o foro da comarca do domicílio do consumidor, nos termos da
                    legislação brasileira.
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
          <h2 className="text-3xl font-extrabold text-white">
            Pronto para explorar nossas plantas?
          </h2>
          <p className="mt-2 text-white/90">
            Conheça os projetos Confort, Prime e Diamond — prontos para download.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to="/produtos"
              className="rounded-xl bg-white text-neutral-900 font-semibold px-6 py-3 hover:bg-neutral-100 transition inline-flex items-center gap-2"
            >
              Ver catálogo <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              to="/contato"
              className="rounded-xl border border-white/40 text-white font-semibold px-6 py-3 hover:bg-white/10 transition"
            >
              Fale conosco
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
