import { Navigate, useLocation } from 'react-router-dom';
import RouteFallback from '../../components/common/RouteFallback';
import { useAuth } from '../../contexts/AuthContext';
import Forbidden from '../../pages/admin/Forbidden';

type Props = {
  children: React.ReactNode;
};

/**
 * Portão do painel — **apenas de experiência**.
 *
 * Qualquer pessoa consegue alterar `isAdmin` no navegador, e o chunk do painel é
 * publicamente baixável. A autorização real é do servidor: toda rota `/v1/admin/**` exige
 * `ROLE_ADMIN`. Este componente existe para que um usuário comum veja um 403 claro em vez
 * de uma tela cheia de requisições falhando.
 */
export default function AdminRoute({ children }: Props) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <RouteFallback label="Verificando acesso…" />;
  }

  if (!isAuthenticated) {
    const redirect = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?redirect=${redirect}`} replace />;
  }

  if (!isAdmin) {
    return <Forbidden />;
  }

  return <>{children}</>;
}
