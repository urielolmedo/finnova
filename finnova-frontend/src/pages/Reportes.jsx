import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import {
  BarChart, Bar, PieChart, Pie, Cell, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';

const COLORES_TORTA = ['#2e7d32', '#c62828', '#1565c0', '#6a1b9a', '#e65100', '#00838f', '#ad1457', '#6c757d'];

function primerDiaDelMes() {
  const hoy = new Date();
  return new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString().split('T')[0];
}
function hoyISO() {
  return new Date().toISOString().split('T')[0];
}

export default function Reportes() {
  const [desde, setDesde] = useState(primerDiaDelMes());
  const [hasta, setHasta] = useState(hoyISO());
  const [resumen, setResumen] = useState(null);
  const [porCategoria, setPorCategoria] = useState([]);
  const [evolucion, setEvolucion] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [linkCompartido, setLinkCompartido] = useState('');
  const [tituloReporte, setTituloReporte] = useState('Reporte personalizado');

  const cargarDatos = async () => {
    setCargando(true);
    setError('');
    try {
      const [resResumen, resCategoria] = await Promise.all([
        api.get(`/reportes/balance?desde=${desde}&hasta=${hasta}`),
        api.get(`/reportes/por-categoria?desde=${desde}&hasta=${hasta}`),
      ]);
      setResumen(resResumen.data);
      setPorCategoria(resCategoria.data);

      // La evolucion mensual requiere al menos 2 meses de diferencia; si no, la omitimos sin romper la pantalla
      try {
        const resEvolucion = await api.get(`/reportes/evolucion?desde=${desde}&hasta=${hasta}`);
        setEvolucion(resEvolucion.data);
      } catch {
        setEvolucion([]);
      }
    } catch (err) {
      setError(err.response?.data || 'No se pudo generar el reporte');
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarDatos();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const aplicarPeriodo = (e) => {
    e.preventDefault();
    cargarDatos();
  };

  const exportarPdf = async () => {
    const response = await api.get(
      `/reportes/exportar/pdf?desde=${desde}&hasta=${hasta}&titulo=${encodeURIComponent(tituloReporte)}`,
      { responseType: 'blob' }
    );
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = blobUrl;
    link.setAttribute('download', 'reporte.pdf');
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  const exportarCsv = async () => {
    const response = await api.get(
      `/reportes/exportar/csv?desde=${desde}&hasta=${hasta}&titulo=${encodeURIComponent(tituloReporte)}`,
      { responseType: 'blob' }
    );
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = blobUrl;
    link.setAttribute('download', 'reporte.csv');
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  const compartirReporte = async () => {
    const { data } = await api.post(
      `/reportes/compartir?desde=${desde}&hasta=${hasta}&titulo=${encodeURIComponent(tituloReporte)}`
    );
    setLinkCompartido(data.urlPublica);
  };

  const copiarLink = () => {
    navigator.clipboard.writeText(linkCompartido);
    alert('Link copiado al portapapeles');
  };

  const datosBarras = resumen ? [
    { nombre: 'Ingresos', monto: resumen.totalIngresos, fill: '#2e7d32' },
    { nombre: 'Egresos', monto: resumen.totalEgresos, fill: '#c62828' },
  ] : [];

  const datosTorta = porCategoria
    .map((c) => ({ nombre: c.categoriaNombre, valor: c.totalIngresos + c.totalEgresos }))
    .filter((c) => c.valor > 0);

  return (
    <div style={{ maxWidth: 900, margin: '40px auto', fontFamily: 'sans-serif', padding: '0 16px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>FinNova</h1>
        <Link to="/transacciones">← Volver a Transacciones</Link>
      </div>
      <h2>Reportes y Resúmenes Financieros</h2>

      <form onSubmit={aplicarPeriodo} style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 16 }}>
        <label>Desde: </label>
        <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        <label>Hasta: </label>
        <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        <input
          type="text"
          placeholder="Título del reporte"
          value={tituloReporte}
          onChange={(e) => setTituloReporte(e.target.value)}
          style={{ flex: 1, minWidth: 160 }}
        />
        <button type="submit">Generar</button>
      </form>

      {error && <p style={{ color: 'red' }}>{String(error)}</p>}

      {cargando ? (
        <p>Cargando...</p>
      ) : resumen && (
        <>
          {/* Resumen numerico */}
          <div style={{ display: 'flex', gap: 24, margin: '16px 0' }}>
            <div style={{ padding: 12, background: '#e8f5e9', borderRadius: 6, color: '#2e7d32' }}>
              <strong>Ingresos:</strong> ${resumen.totalIngresos.toFixed(2)}
            </div>
            <div style={{ padding: 12, background: '#ffebee', borderRadius: 6, color: '#c62828' }}>
              <strong>Egresos:</strong> ${resumen.totalEgresos.toFixed(2)}
            </div>
            <div style={{
              padding: 12, borderRadius: 6,
              background: resumen.superavit ? '#e8f5e9' : '#ffebee',
              color: resumen.superavit ? '#2e7d32' : '#c62828',
            }}>
              <strong>Balance ({resumen.superavit ? 'superávit' : 'déficit'}):</strong> ${resumen.balance.toFixed(2)}
            </div>
          </div>

          {/* Botones de accion */}
          <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
            <button onClick={exportarPdf}>📄 Exportar PDF</button>
            <button onClick={exportarCsv}>📊 Exportar Excel/CSV</button>
            <button onClick={compartirReporte}>🔗 Compartir</button>
          </div>

          {linkCompartido && (
            <div style={{ background: '#f8f9fa', padding: 12, borderRadius: 6, marginBottom: 24 }}>
              <p style={{ margin: '0 0 8px' }}>Link para compartir (válido por 24hs):</p>
              <div style={{ display: 'flex', gap: 8 }}>
                <input readOnly value={linkCompartido} style={{ flex: 1, padding: 6 }} />
                <button onClick={copiarLink}>Copiar</button>
              </div>
            </div>
          )}

          {/* Grafico de barras */}
          <h3>Ingresos vs Egresos</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={datosBarras}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="nombre" />
              <YAxis />
              <Tooltip formatter={(v) => `$${v.toFixed(2)}`} />
              <Bar dataKey="monto">
                {datosBarras.map((entry, index) => (
                  <Cell key={index} fill={entry.fill} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>

          {/* Grafico de torta por categoria */}
          {datosTorta.length > 0 && (
            <>
              <h3>Distribución por categoría</h3>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie data={datosTorta} dataKey="valor" nameKey="nombre" cx="50%" cy="50%" outerRadius={100} label>
                    {datosTorta.map((entry, index) => (
                      <Cell key={index} fill={COLORES_TORTA[index % COLORES_TORTA.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v) => `$${v.toFixed(2)}`} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </>
          )}

          {/* Tabla por categoria */}
          {porCategoria.length > 0 && (
            <>
              <h3>Desglose por categoría</h3>
              <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 24 }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '2px solid #ccc' }}>
                    <th style={{ padding: 8 }}>Categoría</th>
                    <th style={{ padding: 8 }}>Ingresos</th>
                    <th style={{ padding: 8 }}>Egresos</th>
                  </tr>
                </thead>
                <tbody>
                  {porCategoria.map((c, i) => (
                    <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                      <td style={{ padding: 8 }}>{c.categoriaNombre}</td>
                      <td style={{ padding: 8, color: '#2e7d32' }}>${c.totalIngresos.toFixed(2)}</td>
                      <td style={{ padding: 8, color: '#c62828' }}>${c.totalEgresos.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}

          {/* Evolucion en el tiempo (solo si el rango cubre 2+ meses) */}
          {evolucion.length > 0 && (
            <>
              <h3>Evolución financiera</h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={evolucion}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="periodo" />
                  <YAxis />
                  <Tooltip formatter={(v) => `$${v.toFixed(2)}`} />
                  <Legend />
                  <Line type="monotone" dataKey="ingresos" stroke="#2e7d32" name="Ingresos" />
                  <Line type="monotone" dataKey="egresos" stroke="#c62828" name="Egresos" />
                  <Line type="monotone" dataKey="balance" stroke="#1565c0" name="Balance" />
                </LineChart>
              </ResponsiveContainer>
            </>
          )}
        </>
      )}
    </div>
  );
}