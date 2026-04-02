import {
  ArrowRight,
  CheckCircle2,
  FileText,
  Handshake,
  Layers3,
  Mail,
  ShieldCheck,
} from 'lucide-react';

export default function Carrer() {
  const benefits = [
    {
      icon: <Handshake className="h-5 w-5" />,
      title: 'Parceria comercial',
      description:
        'Publique seus projetos na PlantaHUB e negocie uma margem de venda justa para cada planta comercializada.',
    },
    {
      icon: <Layers3 className="h-5 w-5" />,
      title: 'Mais alcance para seus projetos',
      description:
        'Sua planta pode ser encontrada por clientes que buscam soluções arquitetônicas prontas, com exposição contínua dentro da plataforma.',
    },
    {
      icon: <ShieldCheck className="h-5 w-5" />,
      title: 'Distribuição organizada',
      description:
        'Centralizamos o catálogo, o acesso do cliente e o fluxo de entrega digital, tornando a venda mais profissional e escalável.',
    },
  ];

  const steps = [
    {
      number: '01',
      title: 'Envie seu portfólio',
      description:
        'Compartilhe exemplos das suas plantas, estilos de projeto, informações técnicas e materiais que representem a qualidade do seu trabalho.',
    },
    {
      number: '02',
      title: 'Analisamos a compatibilidade',
      description:
        'Nossa equipe avalia o potencial comercial, a organização dos arquivos e o alinhamento dos seus projetos com o padrão da plataforma.',
    },
    {
      number: '03',
      title: 'Negociamos a parceria',
      description:
        'Definimos juntos a margem de venda, regras de publicação, critérios de atualização e os formatos dos arquivos comercializados.',
    },
    {
      number: '04',
      title: 'Publicamos seus projetos',
      description:
        'Depois da aprovação, sua coleção entra no catálogo e passa a ficar disponível para clientes da PlantaHUB.',
    },
  ];

  const requirements = [
    'Plantas organizadas por disciplina ou categoria técnica',
    'Arquivos com nomenclatura clara e estrutura profissional',
    'Projetos autorais ou com autorização formal de comercialização',
    'Material de apresentação com boa qualidade visual',
  ];

  return (
    <section className="bg-white">
      <div className="border-b border-neutral-200 bg-neutral-50">
        <div className="mx-auto max-w-7xl px-6 py-16 md:py-20">
          <div className="max-w-3xl">
            <span className="inline-flex rounded-full bg-orange-50 px-4 py-2 text-sm font-semibold text-primary-600">
              Trabalhe conosco
            </span>

            <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-neutral-900 md:text-5xl">
              Publique suas plantas na PlantaHUB e transforme seus projetos em novas oportunidades
              de venda
            </h1>

            <p className="mt-5 text-lg leading-relaxed text-neutral-600">
              Estamos em busca de arquitetos, projetistas e parceiros que desejam comercializar
              plantas com organização, alcance digital e negociação transparente de margem.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <a
                href="mailto:parcerias@plantahub.com.br?subject=Quero%20publicar%20minhas%20plantas%20na%20PlantaHUB"
                className="inline-flex items-center gap-2 rounded-2xl bg-primary-500 px-6 py-3 font-semibold text-white transition hover:bg-primary-600"
              >
                Enviar proposta
                <ArrowRight className="h-4 w-4" />
              </a>

              <a
                href="#como-funciona"
                className="inline-flex items-center rounded-2xl border border-neutral-300 bg-white px-6 py-3 font-semibold text-neutral-900 transition hover:bg-neutral-100"
              >
                Entender o processo
              </a>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-6 py-16">
        <div className="grid gap-6 md:grid-cols-3">
          {benefits.map(item => (
            <div
              key={item.title}
              className="rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-orange-100 bg-orange-50 text-primary-600">
                {item.icon}
              </div>

              <h2 className="mt-4 text-lg font-extrabold text-neutral-900">{item.title}</h2>
              <p className="mt-2 text-sm leading-relaxed text-neutral-600">{item.description}</p>
            </div>
          ))}
        </div>
      </div>

      <div id="como-funciona" className="bg-neutral-50">
        <div className="mx-auto max-w-7xl px-6 py-16">
          <div className="max-w-3xl">
            <h2 className="text-3xl font-extrabold text-neutral-900">Como funciona a parceria</h2>
            <p className="mt-3 text-neutral-600">
              Nosso processo foi pensado para facilitar a entrada de novos parceiros e garantir
              consistência no catálogo da plataforma.
            </p>
          </div>

          <div className="mt-10 grid gap-6 md:grid-cols-2">
            {steps.map(step => (
              <div
                key={step.number}
                className="rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm"
              >
                <div className="text-sm font-extrabold text-primary-600">{step.number}</div>
                <h3 className="mt-2 text-xl font-extrabold text-neutral-900">{step.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-neutral-600">{step.description}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-6 py-16">
        <div className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-orange-100 bg-orange-50 text-primary-600">
                <FileText className="h-5 w-5" />
              </div>
              <h2 className="text-2xl font-extrabold text-neutral-900">O que esperamos receber</h2>
            </div>

            <div className="mt-6 space-y-4">
              {requirements.map(item => (
                <div key={item} className="flex items-start gap-3">
                  <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-green-600" />
                  <p className="text-sm leading-relaxed text-neutral-700">{item}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-3xl border border-neutral-200 bg-neutral-50 p-8 shadow-sm">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-orange-100 bg-white text-primary-600">
                <Mail className="h-5 w-5" />
              </div>
              <h2 className="text-2xl font-extrabold text-neutral-900">Canal de contato</h2>
            </div>

            <p className="mt-4 text-sm leading-relaxed text-neutral-600">
              Envie sua apresentação, links, portfólio e uma breve explicação sobre os tipos de
              plantas que você deseja comercializar.
            </p>

            <div className="mt-6 rounded-2xl border border-neutral-200 bg-white p-5">
              <div className="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                E-mail sugerido
              </div>
              <a
                href="mailto:parcerias@plantahub.com.br?subject=Quero%20publicar%20minhas%20plantas%20na%20PlantaHUB"
                className="mt-2 block text-lg font-extrabold text-primary-600 hover:underline"
              >
                parcerias@plantahub.com.br
              </a>
            </div>

            <div className="mt-6">
              <a
                href="mailto:parcerias@plantahub.com.br?subject=Quero%20publicar%20minhas%20plantas%20na%20PlantaHUB"
                className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-primary-500 px-5 py-3 font-semibold text-white transition hover:bg-primary-600"
              >
                Entrar em contato
                <ArrowRight className="h-4 w-4" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
