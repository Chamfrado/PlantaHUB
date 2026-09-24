import { BadgeCheck, Facebook, Instagram, Linkedin, Lock, Youtube } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAsync } from '../../hooks/useAsync';
import { listCategories } from '../../services/categories.service';
import { useSiteSettings } from '../../hooks/useSitePage';

type LinkItem = { label: string; href: string };

export default function Footer() {
  // A promise de categorias é memoizada no serviço, então o rodapé — presente em toda
  // página — compartilha a mesma requisição com o resto do app.
  const { data: categories } = useAsync('categories', () => listCategories());

  // Memoizado no servico pelo mesmo motivo das categorias: o rodape esta em toda pagina.
  const settings = useSiteSettings();

  const products: LinkItem[] = (categories ?? []).map(c => ({
    label: c.name,
    href: `/produtos?category=${encodeURIComponent(c.slug)}`,
  }));

  const company: LinkItem[] = [
    { label: 'Sobre Nós', href: '/sobre' },
    { label: 'Como Funciona', href: '/sobre' },
    { label: 'Trabalhe Conosco', href: '/trabalhe-conosco' },
  ];

  const support: LinkItem[] = [
    { label: 'FAQ', href: '/faq' },
    { label: 'Contato', href: '/contato' },
    { label: 'Termos de Serviço', href: '/legal/termos' },
    { label: 'Política de Privacidade', href: '/legal/privacidade' },
  ];

  return (
    <footer className="bg-brand-graphite text-slate-200">
      <div className="mx-auto max-w-7xl px-6 pb-8 pt-14">
        <div className="grid gap-12 lg:grid-cols-4">
          <div>
            <div className="flex items-center">
              <img
                src="/brand/logo-negative.png"
                alt="PlantaHUB logo"
                className="h-14 w-auto object-contain"
              />
            </div>

            <p className="mt-4 max-w-sm text-sm leading-relaxed text-slate-400">
              Plantas arquitetônicas profissionais para projetos residenciais. Documentação técnica
              organizada, experiência digital e acesso simplificado à sua biblioteca.
            </p>

            {/* Os links vinham cravados aqui e, de novo, na pagina de Contato — e os dois
                discordavam: Instagram e Facebook apontavam para perfis diferentes. Dado
                repetido nao fica igual, fica igual ate alguem mudar um lado. Rede sem URL
                cadastrada simplesmente nao aparece. */}
            <div className="mt-6 flex items-center gap-3">
              {settings?.facebookUrl ? (
                <SocialLink href={settings.facebookUrl} label="Facebook" icon={Facebook} />
              ) : null}

              {settings?.instagramUrl ? (
                <SocialLink href={settings.instagramUrl} label="Instagram" icon={Instagram} />
              ) : null}

              {settings?.linkedinUrl ? (
                <SocialLink href={settings.linkedinUrl} label="LinkedIn" icon={Linkedin} />
              ) : null}

              {settings?.youtubeUrl ? (
                <SocialLink href={settings.youtubeUrl} label="YouTube" icon={Youtube} />
              ) : null}
            </div>
          </div>

          <FooterColumn title="Produtos" links={products} />
          <FooterColumn title="Empresa" links={company} />
          <FooterColumn title="Suporte" links={support} />
        </div>

        <div className="mt-12 border-t border-slate-800/70" />

        <div className="mt-6 flex flex-col gap-4 text-sm text-slate-400 md:flex-row md:items-center md:justify-between">
          <span>© {new Date().getFullYear()} PlantaHUB. Todos os direitos reservados.</span>

          <div className="flex items-center gap-6">
            <Badge icon={Lock} label="SSL Secured" />
            <Badge icon={BadgeCheck} label="Trusted Platform" />
          </div>
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({ title, links }: { title: string; links: LinkItem[] }) {
  return (
    <div>
      <div className="font-semibold text-slate-200">{title}</div>
      <ul className="mt-4 space-y-3">
        {links.map(link => (
          <li key={link.label}>
            <Link to={link.href} className="text-sm text-slate-400 transition hover:text-white">
              {link.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

function SocialLink({
  href,
  label,
  icon: Icon,
}: {
  href: string;
  label: string;
  icon: React.ElementType;
}) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={label}
      className="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-800 bg-slate-900 text-slate-300 transition hover:border-slate-700 hover:text-white"
    >
      <Icon className="h-4 w-4" />
    </a>
  );
}

function Badge({ icon: Icon, label }: { icon: React.ElementType; label: string }) {
  return (
    <div className="inline-flex items-center gap-2 text-slate-400">
      <Icon className="h-4 w-4 text-brand-green" />
      <span>{label}</span>
    </div>
  );
}
