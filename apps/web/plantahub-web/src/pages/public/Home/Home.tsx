import CategoryShowcase from '../../../components/home/CategoryShowcase';
import FinalCTA from '../../../components/home/FinalCTA';
import Hero from '../../../components/home/Hero';
import HowItWorks from '../../../components/home/HowItWorks';
import WhyChoose from '../../../components/home/WhyChoose';
import { useAsync } from '../../../hooks/useAsync';
import { listCategories } from '../../../services/categories.service';

export default function Home() {
  const coverImageUrl =
    'https://plantahub-assets.s3.us-east-2.amazonaws.com/gallery/hero-cover.webp';

  // As vitrines saem do banco: marcar uma categoria como destaque no painel passa a
  // bastar para ela aparecer aqui, sem deploy.
  const { data: categories } = useAsync('categories', () => listCategories());

  const featured = (categories ?? [])
    .filter(c => c.featuredOnHome)
    .sort((a, b) => a.homeOrder - b.homeOrder);

  return (
    <div className="min-h-screen flex flex-col bg-brand-light">
      <main className="flex-1">
        <Hero imageSrc={coverImageUrl} />
        <WhyChoose />

        {featured.map(category => (
          <CategoryShowcase
            key={category.slug}
            categorySlug={category.slug}
            title={category.name}
            subtitle={category.description ?? undefined}
            ctaLabel={`Ver tudo em ${category.name}`}
          />
        ))}

        <HowItWorks />
        <FinalCTA />
      </main>
    </div>
  );
}
