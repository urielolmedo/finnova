import { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import TransaccionForm from '../components/TransaccionForm';

const NOMBRES_MES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

function primerDiaDelMes(anio, mes) {
  return new Date(anio, mes, 1).toISOString().split('T')[0];
}
function ultimoDiaDelMes(anio, mes) {
  return new Date(anio, mes + 1, 0).toISOString().split('T')[0];
}

export default function Transacciones() {
  const { usuario, logout } = useAuth();
  const [transacciones, setTransacciones] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [mostrarForm, setMostrarForm] = useState(false);
  const [editando, setEditando] = useState(null);
  const formRef = useRef(null);

  // Vista: por defecto, mes actual. "todo" = historial completo sin filtro de fecha.
  const hoy = new Date();
  const [anioVisible, setAnioVisible] = useState(hoy.getFullYear());
  const [mesVisible, setMesVisible] = useState(hoy.getMonth());
  const [modoVista, setModoVista] = useState('mes'); // 'mes' | 'todo'

  // Filtro adicional por categoria (se aplica sobre la vista actual: mes o todo)
  const [filtroCategoria, setFiltroCategoria] = useState('');

  const cargarCategorias = async () => {
    const { data } = await api.get('/categorias');
    setCategorias(data);
  };

  const cargarTransacciones = async () => {
    setCargando(true);
    let url;

    if (filtroCategoria) {
      url = `/transacciones/filtrar/categoria/${filtroCategoria}`;
    } else if (modoVista === 'mes') {
      const desde = primerDiaDelMes(anioVisible, mesVisible);
      const hasta = ultimoDiaDelMes(anioVisible, mesVisible);
      url = `/transacciones/filtrar/fecha?desde=${desde}&hasta=${hasta}`;
    } else {
      url = '/transacciones';
    }

    const { data } = await api.get(url);
    setTransacciones(data);
    setCargando(false);
  };

  useEffect(() => {
    cargarCategorias();
  }, []);

  useEffect(() => {
    cargarTransacciones();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [anioVisible, mesVisible, modoVista, filtroCategoria]);

  const irMesAnterior = () => {
    setModoVista('mes');
    setFiltroCategoria('');
    if (mesVisible === 0) {
      setMesVisible(11);
      setAnioVisible((a) => a - 1);
    } else {
      setMesVisible((m) => m - 1);
    }
  };

  const irMesSiguiente = () => {
    setModoVista('mes');
    setFiltroCategoria('');
    if (mesVisible === 11) {
      setMesVisible(0);
      setAnioVisible((a) => a + 1);
    } else {
      setMesVisible((m) => m + 1);
    }
  };

  const irMesActual = () => {
    setModoVista('mes');
    setFiltroCategoria('');
    setAnioVisible(hoy.getFullYear());
    setMesVisible(hoy.getMonth());
  };

  const verTodoElHistorial = () => {
    setModoVista('todo');
    setFiltroCategoria('');
  };

  const eliminar = async (id) => {
    if (!confirm('¿Eliminar esta transacción?')) return;
    await api.delete(`/transacciones/${id}`);
    cargarTransacciones();
  };

  const scrollAlFormulario = () => {
    setTimeout(() => {
      formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 50);
  };

  const editar = (t) => {
    setEditando(t);
    setMostrarForm(true);
    scrollAlFormulario();
  };

  const nuevaTransaccion = () => {
    setEditando(null);
    setMostrarForm(true);
    scrollAlFormulario();
  };

  const alGuardar = () => {
    setMostrarForm(false);
    setEditando(null);
    cargarTransacciones();
  };

  const totalIngresos = transacciones.filter((t) => t.tipo === 'INGRESO').reduce((s, t) => s + t.monto, 0);
  const totalEgresos = transacciones.filter((t) => t.tipo === 'EGRESO').reduce((s, t) => s + t.monto, 0);

  const tituloVista = filtroCategoria
    ? `Filtrado por categoría`
    : modoVista === 'mes'
    ? `${NOMBRES_MES[mesVisible]} ${anioVisible}`
    : 'Todo el historial';

  return (
    <div style={{ maxWidth: 900, margin: '40px auto', fontFamily: 'sans-serif', padding: '0 16px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>FinNova</h1>
        <div>
          <Link to="/perfil" style={{ marginRight: 16 }}>Mi perfil</Link>
          <Link to="/categorias" style={{ marginRight: 16 }}>Categorías</Link>
          <Link to="/datos" style={{ marginRight: 16 }}>Exportar/Importar</Link>
          <Link to="/reportes" style={{ marginRight: 16 }}>Reportes</Link>
          <Link to="/tipo-cambio" style={{ marginRight: 16 }}>Tipo de Cambio</Link>
          <button onClick={logout}>Cerrar sesión</button>
        </div>
      </div>
      <p>Hola, {usuario?.nombre}</p>

      {/* Navegación mes a mes */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '16px 0 8px' }}>
        <button onClick={irMesAnterior}>← Mes anterior</button>
        <h2 style={{ margin: 0, minWidth: 220, textAlign: 'center' }}>{tituloVista}</h2>
        <button onClick={irMesSiguiente}>Mes siguiente →</button>
        {!(modoVista === 'mes' && anioVisible === hoy.getFullYear() && mesVisible === hoy.getMonth()) && (
          <button onClick={irMesActual}>Ir al mes actual</button>
        )}
      </div>
      <p style={{ fontSize: 13 }}>
        {modoVista === 'mes' ? (
          <>¿Buscás comparar varios meses o ver la tendencia en el tiempo? Eso está en <Link to="/reportes">Reportes</Link>.{' · '}</>
        ) : null}
        <button onClick={modoVista === 'todo' ? irMesActual : verTodoElHistorial} style={{ fontSize: 13, padding: '2px 8px' }}>
          {modoVista === 'todo' ? 'Volver a vista mensual' : 'Ver todo el historial'}
        </button>
      </p>

      {/* Resumen del periodo que se esta viendo (nunca "global") */}
      <div style={{ display: 'flex', gap: 24, margin: '16px 0' }}>
        <div style={{ padding: 12, background: '#e8f5e9', borderRadius: 6 }}>
          <strong>Ingresos:</strong> ${totalIngresos.toFixed(2)}
        </div>
        <div style={{ padding: 12, background: '#ffebee', borderRadius: 6 }}>
          <strong>Egresos:</strong> ${totalEgresos.toFixed(2)}
        </div>
        <div style={{ padding: 12, background: '#e3f2fd', borderRadius: 6 }}>
          <strong>Balance:</strong> ${(totalIngresos - totalEgresos).toFixed(2)}
        </div>
      </div>
      <p style={{ fontSize: 12, color: '#888', marginTop: -8 }}>
        Estos totales corresponden solo a: <strong>{tituloVista}</strong>.
      </p>

      <button onClick={nuevaTransaccion} style={{ padding: '8px 16px', marginBottom: 16 }}>
        + Nueva transacción
      </button>

      {mostrarForm && (
        <div ref={formRef}>
          <TransaccionForm
            categorias={categorias}
            transaccion={editando}
            onGuardado={alGuardar}
            onCancelar={() => setMostrarForm(false)}
          />
        </div>
      )}

      <h3>Filtrar por categoría</h3>
      <select
        value={filtroCategoria}
        onChange={(e) => setFiltroCategoria(e.target.value)}
        style={{ marginBottom: 16 }}
      >
        <option value="">Sin filtro de categoría (usar vista actual)</option>
        {categorias.map((c) => (
          <option key={c.id} value={c.id}>{c.nombre}</option>
        ))}
      </select>
      {filtroCategoria && (
        <p style={{ fontSize: 12, color: '#b45309' }}>
          Nota: el filtro por categoría muestra resultados de todo el historial, no solo del mes seleccionado.
        </p>
      )}

      <h3>Historial — {tituloVista}</h3>
      {cargando ? (
        <p>Cargando...</p>
      ) : transacciones.length === 0 ? (
        <p>No hay transacciones registradas en este período.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '2px solid #ccc' }}>
              <th style={{ padding: 8 }}>Fecha</th>
              <th style={{ padding: 8 }}>Tipo</th>
              <th style={{ padding: 8 }}>Categoría</th>
              <th style={{ padding: 8 }}>Descripción</th>
              <th style={{ padding: 8 }}>Monto</th>
              <th style={{ padding: 8 }}>Comprobante</th>
              <th style={{ padding: 8 }}>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {transacciones.map((t) => (
              <tr
                key={t.id}
                style={{
                  borderBottom: '1px solid #eee',
                  background: editando?.id === t.id ? '#fff3cd' : 'transparent',
                }}
              >
                <td style={{ padding: 8 }}>{t.fecha}{t.esRecurrente && ' 🔁'}</td>
                <td style={{ padding: 8, color: t.tipo === 'INGRESO' ? 'green' : 'red' }}>{t.tipo}</td>
                <td style={{ padding: 8 }}>{t.categoriaNombre}</td>
                <td style={{ padding: 8 }}>{t.descripcion}</td>
                <td style={{ padding: 8 }}>${t.monto.toFixed(2)}</td>
                <td style={{ padding: 8 }}>
                  {t.tieneComprobante ? (
                    <a
                      href={`http://localhost:8080/uploads/comprobantes/${t.comprobanteUrl}`}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      📎 Ver
                    </a>
                  ) : '—'}
                </td>
                <td style={{ padding: 8 }}>
                  <button onClick={() => editar(t)}>Editar</button>{' '}
                  <button onClick={() => eliminar(t.id)}>Eliminar</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
