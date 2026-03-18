import { CreditCard, User } from 'lucide-react';
import { useState } from 'react';
import AccountTab from '../../components/preferences/AccountTab';
import TransactionsTab from '../../components/preferences/TransactionTab';

type TabKey = 'account' | 'transactions';

export default function PreferencesPage() {
  const [activeTab, setActiveTab] = useState<TabKey>('account');

  return (
    <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
      <div className="max-w-7xl mx-auto px-6 py-10 md:py-14">
        <div className="max-w-3xl">
          <span className="inline-flex items-center rounded-full bg-orange-50 px-4 py-2 text-sm font-medium text-primary-600">
            Preferências
          </span>

          <h1 className="mt-5 text-3xl md:text-4xl font-extrabold tracking-tight text-neutral-900">
            Configurações da conta
          </h1>

          <p className="mt-3 text-base md:text-lg text-neutral-600 leading-relaxed">
            Gerencie seus dados pessoais e acompanhe o histórico das suas transações.
          </p>
        </div>

        <div className="mt-10 grid gap-8 lg:grid-cols-[260px_minmax(0,1fr)]">
          <aside className="h-fit rounded-2xl border border-neutral-200 bg-white p-3 shadow-sm">
            <SidebarItem
              icon={<User className="h-4 w-4" />}
              title="Conta"
              description="Cadastro, contato e segurança"
              active={activeTab === 'account'}
              onClick={() => setActiveTab('account')}
            />

            <SidebarItem
              icon={<CreditCard className="h-4 w-4" />}
              title="Transações"
              description="Pedidos e pagamentos"
              active={activeTab === 'transactions'}
              onClick={() => setActiveTab('transactions')}
            />
          </aside>

          <main>{activeTab === 'account' ? <AccountTab /> : <TransactionsTab />}</main>
        </div>
      </div>
    </section>
  );
}

function SidebarItem({
  icon,
  title,
  description,
  active,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  active?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        'w-full text-left rounded-2xl px-4 py-4 transition border',
        active
          ? 'bg-orange-50 border-orange-200'
          : 'bg-white border-transparent hover:bg-neutral-50',
      ].join(' ')}
    >
      <div className="flex items-start gap-3">
        <div
          className={[
            'mt-0.5 inline-flex h-9 w-9 items-center justify-center rounded-xl',
            active ? 'bg-white text-primary-600' : 'bg-neutral-100 text-neutral-600',
          ].join(' ')}
        >
          {icon}
        </div>

        <div>
          <div
            className={['text-sm font-bold', active ? 'text-primary-700' : 'text-neutral-900'].join(
              ' '
            )}
          >
            {title}
          </div>
          <p className="mt-1 text-xs leading-relaxed text-neutral-500">{description}</p>
        </div>
      </div>
    </button>
  );
}
