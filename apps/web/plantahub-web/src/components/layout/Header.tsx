import logotipo from '../../assets/logotipo.png';

export default function Header() {
  return (
    <header className="w-full border-b border-gray-300 bg-white">
      <div className="w-full flex items-center px-0 py-4">
        <div className="flex-1 flex items-center justify-start gap-4">
          <img src={logotipo} alt="PlantaHUB" className="h-20" />
          <h1 className="text-xl font-bold text-primary-500">PlantaHUB</h1>
          <nav className="flex-1 flex items-center justify-start text-sm font-medium pl-7">
            <div className="flex gap-6">
              <a
                href="#"
                className="transition-transform duration-300 hover:text-primary-500 hover:scale-110"
              >
                Plantas
              </a>
              <a
                href="#"
                className="transition-transform duration-300 hover:text-primary-500 hover:scale-110"
              >
                Como funciona
              </a>
              <a
                href="#"
                className="transition-transform duration-300 hover:text-primary-500 hover:scale-110"
              >
                Contato
              </a>
            </div>
          </nav>
        </div>

        <div className="flex-1 flex items-center justify-end p-5">
          <h1 className="cursor-pointer hover:text-primary-500 pr-3.5">Cadastrar</h1>
          <button className="px-4 py-2 rounded-lg bg-primary-500 text-white hover:bg-primary-600 cursor-pointer">
            Entrar
          </button>
        </div>
      </div>
    </header>
  );
}
