import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import RutaProtegida from './components/RutaProtegida';
import Login from './pages/Login';
import Registro from './pages/Registro';
import RecuperarPassword from './pages/RecuperarPassword';
import ResetearPassword from './pages/ResetearPassword';
import Perfil from './pages/Perfil';
import Transacciones from './pages/Transacciones';
import Categorias from './pages/Categorias';
import DatosImportExport from './pages/DatosImportExport';
import Reportes from './pages/Reportes';
import ReporteCompartido from './pages/ReporteCompartido';
import TipoCambio from './pages/TipoCambio';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Registro />} />
          <Route path="/recuperar-password" element={<RecuperarPassword />} />
          <Route path="/resetear-password" element={<ResetearPassword />} />
          <Route path="/perfil" element={<RutaProtegida><Perfil /></RutaProtegida>} />
          <Route path="/transacciones" element={<RutaProtegida><Transacciones /></RutaProtegida>} />
          <Route path="/categorias" element={<RutaProtegida><Categorias /></RutaProtegida>} />
          <Route path="/datos" element={<RutaProtegida><DatosImportExport /></RutaProtegida>} />
          <Route path="/reportes" element={<RutaProtegida><Reportes /></RutaProtegida>} />
          <Route path="/reporte-compartido/:token" element={<ReporteCompartido />} />
          <Route path="/tipo-cambio" element={<RutaProtegida><TipoCambio /></RutaProtegida>} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;