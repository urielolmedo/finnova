import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axios';

export default function DatosImportExport() {
  // Exportar CSV
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');

  // Importar CSV
  const [archivoCsv, setArchivoCsv] = useState(null);
  const [previewCsv, setPreviewCsv] = useState(null);
  const [errorCsv, setErrorCsv] = useState('');
  const [cargandoCsv, setCargandoCsv] = useState(false);
  const [mensajeCsv, setMensajeCsv] = useState('');

  // Respaldo
  const [archivoRespaldo, setArchivoRespaldo] = useState(null);
  const [previewRespaldo, setPreviewRespaldo] = useState(null);
  const [errorRespaldo, setErrorRespaldo] = useState('');
  const [cargandoRespaldo, setCargandoRespaldo] = useState(false);
  const [mensajeRespaldo, setMensajeRespaldo] = useState('');

  // --- CU-070: Exportar CSV ---
  const exportarCsv = async () => {
    let url = '/exportar/csv';
    if (desde && hasta) url += `?desde=${desde}&hasta=${hasta}`;
    const response = await api.get(url, { responseType: 'blob' });
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = blobUrl;
    link.setAttribute('download', 'transacciones.csv');
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  // --- CU-072: Exportar respaldo completo ---
  const exportarRespaldo = async () => {
    const response = await api.get('/exportar/respaldo', { responseType: 'blob' });
    const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = blobUrl;
    link.setAttribute('download', 'respaldo_finnova.json');
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  // --- CU-071 (paso 1): Vista previa de importacion CSV ---
  const previsualizarCsv = async () => {
    if (!archivoCsv) return;
    setErrorCsv('');
    setMensajeCsv('');
    setCargandoCsv(true);
    try {
      const formData = new FormData();
      formData.append('archivo', archivoCsv);
      const { data } = await api.post('/exportar/importar/csv/preview', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setPreviewCsv(data);
    } catch (err) {
      setErrorCsv(err.response?.data || 'No se pudo procesar el archivo');
      setPreviewCsv(null);
    } finally {
      setCargandoCsv(false);
    }
  };

  // --- CU-071 (paso 2): Confirmar importacion CSV ---
  const confirmarCsv = async () => {
    setCargandoCsv(true);
    try {
      const { data } = await api.post('/exportar/importar/csv/confirmar', previewCsv.filasValidas);
      setMensajeCsv(data);
      setPreviewCsv(null);
      setArchivoCsv(null);
    } catch (err) {
      setErrorCsv(err.response?.data || 'No se pudo confirmar la importación');
    } finally {
      setCargandoCsv(false);
    }
  };

  // --- CU-073 (paso 1): Vista previa de restauracion ---
  const previsualizarRespaldo = async () => {
    if (!archivoRespaldo) return;
    setErrorRespaldo('');
    setMensajeRespaldo('');
    setCargandoRespaldo(true);
    try {
      const formData = new FormData();
      formData.append('archivo', archivoRespaldo);
      const { data } = await api.post('/exportar/importar/respaldo/preview', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setPreviewRespaldo(data);
    } catch (err) {
      setErrorRespaldo(err.response?.data || 'No se pudo procesar el archivo');
      setPreviewRespaldo(null);
    } finally {
      setCargandoRespaldo(false);
    }
  };

  // --- CU-073 (paso 2): Confirmar restauracion ---
  const confirmarRespaldo = async () => {
    if (!confirm(
      'Esta acción reemplaza TODAS tus categorías personalizadas y transacciones actuales ' +
      'por las del respaldo, de forma PERMANENTE. ¿Confirmás que querés continuar?'
    )) return;

    setCargandoRespaldo(true);
    try {
      const { data } = await api.post('/exportar/importar/respaldo/confirmar', previewRespaldo.respaldo);
      setMensajeRespaldo(data);
      setPreviewRespaldo(null);
      setArchivoRespaldo(null);
    } catch (err) {
      setErrorRespaldo(err.response?.data || 'No se pudo confirmar la restauración');
    } finally {
      setCargandoRespaldo(false);
    }
  };

  return (
    <div style={{ maxWidth: 700, margin: '40px auto', fontFamily: 'sans-serif', padding: '0 16px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>FinNova</h1>
        <Link to="/transacciones">← Volver a Transacciones</Link>
      </div>
      <h2>Exportar / Importar Datos</h2>

      {/* CU-070: Exportar CSV */}
      <section style={{ border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 24 }}>
        <h3>Exportar transacciones (CSV)</h3>
        <p style={{ fontSize: 14, color: '#666' }}>
          Dejá las fechas vacías para exportar todo el historial, o elegí un rango.
        </p>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 12 }}>
          <label>Desde: </label>
          <input type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
          <label>Hasta: </label>
          <input type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </div>
        <button onClick={exportarCsv}>Descargar CSV</button>
      </section>

      {/* CU-071: Importar CSV */}
      <section style={{ border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 24 }}>
        <h3>Importar transacciones (CSV)</h3>
        <p style={{ fontSize: 14, color: '#666' }}>
          El archivo debe tener las columnas Tipo, Monto, Fecha y Categoria (separadas por ';' o ',').
        </p>
        <input
          type="file"
          accept=".csv"
          onChange={(e) => { setArchivoCsv(e.target.files[0]); setPreviewCsv(null); }}
        />{' '}
        <button onClick={previsualizarCsv} disabled={!archivoCsv || cargandoCsv}>
          {cargandoCsv ? 'Procesando...' : 'Analizar archivo'}
        </button>

        {errorCsv && <p style={{ color: 'red' }}>{String(errorCsv)}</p>}
        {mensajeCsv && <p style={{ color: 'green' }}>{mensajeCsv}</p>}

        {previewCsv && (
          <div style={{ marginTop: 12, background: '#f8f9fa', padding: 12, borderRadius: 6 }}>
            <p><strong>{previewCsv.cantidadValidas}</strong> transacción(es) lista(s) para importar.</p>
            {previewCsv.errores.length > 0 && (
              <div>
                <p style={{ color: '#b45309' }}><strong>{previewCsv.errores.length}</strong> fila(s) con problemas (se van a omitir):</p>
                <ul style={{ fontSize: 13, maxHeight: 150, overflowY: 'auto' }}>
                  {previewCsv.errores.map((e, i) => <li key={i}>{e}</li>)}
                </ul>
              </div>
            )}
            {previewCsv.cantidadValidas > 0 && (
              <button onClick={confirmarCsv} disabled={cargandoCsv} style={{ marginTop: 8 }}>
                Confirmar importación de {previewCsv.cantidadValidas} transacción(es)
              </button>
            )}
          </div>
        )}
      </section>

      {/* CU-072: Exportar respaldo */}
      <section style={{ border: '1px solid #ddd', borderRadius: 8, padding: 16, marginBottom: 24 }}>
        <h3>Exportar respaldo completo</h3>
        <p style={{ fontSize: 14, color: '#666' }}>
          Descarga un archivo con todas tus categorías personalizadas y transacciones, para
          guardar como copia de seguridad.
        </p>
        <button onClick={exportarRespaldo}>Descargar respaldo (.json)</button>
      </section>

      {/* CU-073: Restaurar respaldo */}
      <section style={{ border: '2px solid #e57373', borderRadius: 8, padding: 16 }}>
        <h3>⚠️ Restaurar desde respaldo</h3>
        <p style={{ fontSize: 14, color: '#666' }}>
          Esto reemplaza TODAS tus categorías personalizadas y transacciones actuales por las
          del archivo. La acción es permanente e irreversible.
        </p>
        <input
          type="file"
          accept=".json"
          onChange={(e) => { setArchivoRespaldo(e.target.files[0]); setPreviewRespaldo(null); }}
        />{' '}
        <button onClick={previsualizarRespaldo} disabled={!archivoRespaldo || cargandoRespaldo}>
          {cargandoRespaldo ? 'Procesando...' : 'Analizar archivo'}
        </button>

        {errorRespaldo && <p style={{ color: 'red' }}>{String(errorRespaldo)}</p>}
        {mensajeRespaldo && <p style={{ color: 'green' }}>{mensajeRespaldo}</p>}

        {previewRespaldo && (
          <div style={{ marginTop: 12, background: '#fff3f3', padding: 12, borderRadius: 6 }}>
            <p>Respaldo generado el: {previewRespaldo.fechaGeneracionRespaldo}</p>
            <p><strong>{previewRespaldo.cantidadCategorias}</strong> categoría(s) personalizada(s)</p>
            <p><strong>{previewRespaldo.cantidadTransacciones}</strong> transacción(es)</p>
            <button onClick={confirmarRespaldo} disabled={cargandoRespaldo} style={{ marginTop: 8, background: '#e57373', color: 'white' }}>
              Confirmar restauración (reemplaza todo)
            </button>
          </div>
        )}
      </section>
    </div>
  );
}
