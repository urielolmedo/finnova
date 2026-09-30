import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';

export default function TipoCambio() {
  const [moduloActivo, setModuloActivo] = useState(null);
  const [cotizacion, setCotizacion] = useState(null);
  const [historial, setHistorial] = useState(null);
  const [analisis, setAnalisis] = useState(null);
  const [sugerencia, setSugerencia] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const [diasHistorial, setDiasHistorial] = useState(7);

  const [umbralOficial, setUmbralOficial] = useState('');
  const [umbralBlue, setUmbralBlue] = useState('');
  const [mensajeUmbral, setMensajeUmbral] = useState('');

  const [montoSimular, setMontoSimular] = useState('');
  const [tipoSimular, setTipoSimular] = useState('blue');
  const [resultadoSimulacion, setResultadoSimulacion] = useState(null);
  const [errorSimulacion, setErrorSimulacion] = useState('');

  const cargarTodo = async () => {
    setCargando(true);
    setError('');
    try {
      const { data: perfil } = await api.get('/usuarios/perfil');
      // El perfil no expone directamente el flag; lo inferimos probando el endpoint de cotizacion
      const activo = await probarSiModuloActivo();
      setModuloActivo(activo);
      if (activo) {
        await cargarDatosDelModulo();
      }
    } catch (err) {
      setError('No se pudo cargar la información');
    } finally {
      setCargando(false);
    }
  };

  const probarSiModuloActivo = async () => {
    try {
      await api.get('/tipo-cambio/actual');
      return true;
    } catch {
      return false;
    }
  };

  const cargarDatosDelModulo = async () => {
    const { data: cot } = await api.get('/tipo-cambio/actual');
    setCotizacion(cot);

    try {
      const { data: hist } = await api.get(`/tipo-cambio/historial?dias=${diasHistorial}`);
      setHistorial(hist);
    } catch {
      setHistorial(null);
    }

    try {
      const { data: an } = await api.get('/tipo-cambio/analisis-comparativo');
      setAnalisis(an);
    } catch {
      setAnalisis(null);
    }

    try {
      const { data: sug } = await api.get('/tipo-cambio/sugerencia');
      setSugerencia(sug);
    } catch {
      setSugerencia(null);
    }
  };

  useEffect(() => {
    cargarTodo();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (moduloActivo) {
      api.get(`/tipo-cambio/historial?dias=${diasHistorial}`)
        .then(({ data }) => setHistorial(data))
        .catch(() => setHistorial(null));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [diasHistorial]);

  const activarModulo = async () => {
    await api.put('/tipo-cambio/activar?activo=true');
    setModuloActivo(true);
    await cargarDatosDelModulo();
  };

  const desactivarModulo = async () => {
    if (!confirm('El historial de cotizaciones no se elimina y podrás consultarlo si reactivás el módulo. ¿Confirmás la desactivación?')) return;
    await api.put('/tipo-cambio/activar?activo=false');
    setModuloActivo(false);
  };

  const guardarUmbral = async (e) => {
    e.preventDefault();
    setMensajeUmbral('');
    try {
      const body = {};
      if (umbralOficial) body.umbralOficial = parseFloat(umbralOficial);
      if (umbralBlue) body.umbralBlue = parseFloat(umbralBlue);
      const { data } = await api.put('/tipo-cambio/umbral', body);
      setMensajeUmbral(data);
    } catch (err) {
      setMensajeUmbral(err.response?.data || 'No se pudo configurar el umbral');
    }
  };

  const simular = async (e) => {
    e.preventDefault();
    setErrorSimulacion('');
    setResultadoSimulacion(null);
    try {
      const { data } = await api.post('/tipo-cambio/simular', {
        montoPesos: parseFloat(montoSimular),
        tipoCotizacion: tipoSimular,
      });
      setResultadoSimulacion(data);
    } catch (err) {
      setErrorSimulacion(err.response?.data || 'No se pudo generar la simulación');
    }
  };

  if (cargando) return <p style={{ textAlign: 'center', marginTop: 80 }}>Cargando...</p>;

  return (
    <div style={{ maxWidth: 800, margin: '40px auto', fontFamily: 'sans-serif', padding: '0 16px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>FinNova</h1>
        <Link to="/transacciones">← Volver a Transacciones</Link>
      </div>
      <h2>Monitoreo de Tipo de Cambio</h2>
      <p style={{ fontSize: 13, color: '#888', background: '#fff8e1', padding: 10, borderRadius: 6 }}>
        ⚠️ La información de este módulo es orientativa y no constituye asesoramiento financiero profesional.
      </p>

      {error && <p style={{ color: 'red' }}>{String(error)}</p>}

      {!moduloActivo ? (
        <div style={{ textAlign: 'center', marginTop: 40 }}>
          <p>Este módulo te permite ver la cotización del dólar oficial y blue, su historial, configurar alertas y simular conversiones.</p>
          <button onClick={activarModulo} style={{ padding: '10px 20px' }}>Activar módulo</button>
        </div>
      ) : (
        <>
          <button onClick={desactivarModulo} style={{ float: 'right', marginBottom: 8 }}>Desactivar módulo</button>

          {/* CU-063: Cotización actual */}
          {cotizacion && (
            <div style={{ display: 'flex', gap: 16, margin: '16px 0', clear: 'both' }}>
              <div style={{ flex: 1, padding: 16, background: '#e3f2fd', borderRadius: 8 }}>
                <h3 style={{ margin: 0 }}>Dólar Oficial</h3>
                <p style={{ fontSize: 22, margin: '8px 0' }}>Compra ${cotizacion.oficial.compra} / Venta ${cotizacion.oficial.venta}</p>
                <small style={{ color: '#666' }}>
                  Actualizado: {new Date(cotizacion.oficial.fechaHora).toLocaleString()}
                  {cotizacion.oficial.desactualizada && ' (última disponible, sin conexión con la API)'}
                </small>
              </div>
              <div style={{ flex: 1, padding: 16, background: '#e8f5e9', borderRadius: 8 }}>
                <h3 style={{ margin: 0 }}>Dólar Blue</h3>
                <p style={{ fontSize: 22, margin: '8px 0' }}>Compra ${cotizacion.blue.compra} / Venta ${cotizacion.blue.venta}</p>
                <small style={{ color: '#666' }}>
                  Actualizado: {new Date(cotizacion.blue.fechaHora).toLocaleString()}
                  {cotizacion.blue.desactualizada && ' (última disponible, sin conexión con la API)'}
                </small>
              </div>
            </div>
          )}

          {/* CU-064: Historial */}
          <h3>Historial de cotización</h3>
          <div style={{ marginBottom: 8 }}>
            {[{ l: '7 días', v: 7 }, { l: '1 mes', v: 30 }, { l: '3 meses', v: 90 }].map((op) => (
              <button
                key={op.v}
                onClick={() => setDiasHistorial(op.v)}
                style={{ marginRight: 8, fontWeight: diasHistorial === op.v ? 'bold' : 'normal' }}
              >
                {op.l}
              </button>
            ))}
          </div>

          {historial ? (
            <>
              {historial.primerRegistroDisponible && (
                <p style={{ fontSize: 13, color: '#b45309' }}>
                  El historial disponible es menor al rango seleccionado. Primer registro: {historial.primerRegistroDisponible}
                </p>
              )}
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={historial.puntos}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="fechaHora" tickFormatter={(v) => new Date(v).toLocaleDateString()} />
                  <YAxis />
                  <Tooltip labelFormatter={(v) => new Date(v).toLocaleString()} formatter={(v) => v ? `$${v}` : '—'} />
                  <Legend />
                  <Line type="monotone" dataKey="oficial" stroke="#1565c0" name="Oficial" dot={false} />
                  <Line type="monotone" dataKey="blue" stroke="#2e7d32" name="Blue" dot={false} />
                </LineChart>
              </ResponsiveContainer>
              <div style={{ display: 'flex', gap: 24, fontSize: 13, color: '#666', marginBottom: 24 }}>
                <span>Oficial: min ${historial.minOficial} / max ${historial.maxOficial} / prom ${historial.promedioOficial}</span>
                <span>Blue: min ${historial.minBlue} / max ${historial.maxBlue} / prom ${historial.promedioBlue}</span>
              </div>
            </>
          ) : (
            <p style={{ color: '#888' }}>Todavía no hay historial suficiente. Se va construyendo automáticamente cada 30 minutos.</p>
          )}

          {/* CU-067: Análisis comparativo */}
          {analisis && (
            <>
              <h3>Análisis comparativo vs promedio histórico</h3>
              {['oficial', 'blue'].map((tipo) => (
                <div key={tipo} style={{ marginBottom: 12 }}>
                  <strong>Dólar {tipo}</strong>
                  {analisis[tipo].diasHistorialDisponible != null ? (
                    <p style={{ fontSize: 13, color: '#888' }}>
                      Historial disponible: {analisis[tipo].diasHistorialDisponible} días (se necesitan 30 para un análisis representativo)
                    </p>
                  ) : (
                    <table style={{ width: '100%', fontSize: 14 }}>
                      <tbody>
                        {analisis[tipo].comparativas.map((c) => (
                          <tr key={c.dias}>
                            <td>Promedio {c.dias} días:</td>
                            <td>{c.promedio != null ? `$${c.promedio}` : '—'}</td>
                            <td style={{ color: c.favorable ? '#2e7d32' : '#c62828' }}>
                              {c.desviacionPorcentual != null ? `${c.desviacionPorcentual}%` : ''}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              ))}
            </>
          )}

          {/* CU-068: Sugerencia */}
          {sugerencia && (
            <div style={{ border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 24, background: '#fafafa' }}>
              <h3 style={{ marginTop: 0 }}>💡 Sugerencia</h3>
              <p>{sugerencia.mensaje}</p>
              <p style={{ fontSize: 13, color: '#666' }}>{sugerencia.criterio}</p>
              <p style={{ fontSize: 12, color: '#999' }}>{sugerencia.aviso}</p>
            </div>
          )}

          {/* CU-065: Configurar umbral */}
          <h3>Configurar alerta de tipo de cambio favorable</h3>
          <form onSubmit={guardarUmbral} style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <label>Umbral oficial: $</label>
            <input type="number" value={umbralOficial} onChange={(e) => setUmbralOficial(e.target.value)} style={{ width: 100 }} />
            <label>Umbral blue: $</label>
            <input type="number" value={umbralBlue} onChange={(e) => setUmbralBlue(e.target.value)} style={{ width: 100 }} />
            <button type="submit">Guardar</button>
          </form>
          {mensajeUmbral && <p style={{ fontSize: 13 }}>{String(mensajeUmbral)}</p>}

          {/* CU-069: Simulador */}
          <h3 style={{ marginTop: 24 }}>Simular compra en pesos vs dólares</h3>
          <form onSubmit={simular} style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <label>Monto en pesos: $</label>
            <input type="number" value={montoSimular} onChange={(e) => setMontoSimular(e.target.value)} required />
            <select value={tipoSimular} onChange={(e) => setTipoSimular(e.target.value)}>
              <option value="oficial">Oficial</option>
              <option value="blue">Blue</option>
            </select>
            <button type="submit">Simular</button>
          </form>
          {errorSimulacion && <p style={{ color: 'red' }}>{String(errorSimulacion)}</p>}
          {resultadoSimulacion && (
            <div style={{ background: '#f0f7ff', padding: 12, borderRadius: 6 }}>
              <p><strong>${resultadoSimulacion.montoPesos}</strong> equivalen hoy a <strong>USD {resultadoSimulacion.equivalenteActual}</strong> ({resultadoSimulacion.tipoCotizacion})</p>
              <table style={{ width: '100%', fontSize: 14 }}>
                <tbody>
                  {resultadoSimulacion.equivalentesHistoricos.map((h) => (
                    <tr key={h.dias}>
                      <td>Al promedio de {h.dias} días:</td>
                      <td>{h.equivalente != null ? `USD ${h.equivalente}` : '—'}</td>
                      <td>{h.diferenciaPorcentual != null ? `(${h.diferenciaPorcentual}% vs actual)` : ''}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <p style={{ fontSize: 12, color: '#999' }}>{resultadoSimulacion.aviso}</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}