import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import axios from 'axios';

export default function ReporteCompartido() {
  const { token } = useParams();
  const [reporte, setReporte] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    axios.get(`http://localhost:8080/api/reportes/compartido/${token}`)
      .then(({ data }) => setReporte(data))
      .catch((err) => setError(err.response?.data || 'No se pudo cargar el reporte'));
  }, [token]);

  if (error) {
    return (
      <div style={{ maxWidth: 500, margin: '80px auto', fontFamily: 'sans-serif', textAlign: 'center' }}>
        <h1>FinNova</h1>
        <p style={{ color: 'red' }}>{String(error)}</p>
      </div>
    );
  }

  if (!reporte) return <p style={{ textAlign: 'center', marginTop: 80 }}>Cargando...</p>;

  return (
    <div style={{ maxWidth: 700, margin: '40px auto', fontFamily: 'sans-serif', padding: '0 16px' }}>
      <h1>FinNova</h1>
      <p style={{ fontSize: 13, color: '#888' }}>Reporte compartido — solo lectura</p>
      <h2>{reporte.titulo}</h2>
      <p style={{ color: '#666' }}>
        Período: {reporte.desde} al {reporte.hasta} · Generado por {reporte.generadoPor}
      </p>

      <div style={{ display: 'flex', gap: 24, margin: '16px 0' }}>
        <div style={{ padding: 12, background: '#e8f5e9', borderRadius: 6, color: '#2e7d32' }}>
          <strong>Ingresos:</strong> ${reporte.resumen.totalIngresos.toFixed(2)}
        </div>
        <div style={{ padding: 12, background: '#ffebee', borderRadius: 6, color: '#c62828' }}>
          <strong>Egresos:</strong> ${reporte.resumen.totalEgresos.toFixed(2)}
        </div>
        <div style={{
          padding: 12, borderRadius: 6,
          background: reporte.resumen.superavit ? '#e8f5e9' : '#ffebee',
          color: reporte.resumen.superavit ? '#2e7d32' : '#c62828',
        }}>
          <strong>Balance:</strong> ${reporte.resumen.balance.toFixed(2)}
        </div>
      </div>

      {reporte.porCategoria.length > 0 && (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '2px solid #ccc' }}>
              <th style={{ padding: 8 }}>Categoría</th>
              <th style={{ padding: 8 }}>Ingresos</th>
              <th style={{ padding: 8 }}>Egresos</th>
            </tr>
          </thead>
          <tbody>
            {reporte.porCategoria.map((c, i) => (
              <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: 8 }}>{c.categoriaNombre}</td>
                <td style={{ padding: 8, color: '#2e7d32' }}>${c.totalIngresos.toFixed(2)}</td>
                <td style={{ padding: 8, color: '#c62828' }}>${c.totalEgresos.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}