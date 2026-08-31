import { useState } from 'react';
import api from '../api/axios';

export default function TransaccionForm({ categorias, transaccion, onGuardado, onCancelar }) {
  const esEdicion = !!transaccion;

  const [form, setForm] = useState({
    tipo: transaccion?.tipo || 'EGRESO',
    monto: transaccion?.monto || '',
    fecha: transaccion?.fecha || new Date().toISOString().split('T')[0],
    categoriaId: transaccion?.categoriaId || '',
    descripcion: transaccion?.descripcion || '',
    esRecurrente: transaccion?.esRecurrente || false,
    frecuencia: transaccion?.frecuencia || 'MENSUAL',
    fechaFinRecurrencia: transaccion?.fechaFinRecurrencia || '',
  });
  const [archivo, setArchivo] = useState(null);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const categoriasFiltradas = categorias.filter(
    (c) => c.tipo === form.tipo || c.tipo === 'AMBOS'
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setCargando(true);
    try {
      const body = {
        tipo: form.tipo,
        monto: parseFloat(form.monto),
        fecha: form.fecha,
        categoriaId: parseInt(form.categoriaId),
        descripcion: form.descripcion,
        esRecurrente: form.esRecurrente,
        frecuencia: form.esRecurrente ? form.frecuencia : null,
        fechaFinRecurrencia: form.esRecurrente ? form.fechaFinRecurrencia : null,
      };

      let idTransaccion;
      if (esEdicion) {
        await api.put(`/transacciones/${transaccion.id}`, body);
        idTransaccion = transaccion.id;
      } else {
        const { data } = await api.post('/transacciones', body);
        idTransaccion = data.transaccion ? data.transaccion.id : data.id;
      }

      // CU-014: si el usuario adjunto un archivo, lo subimos aparte
      if (archivo) {
        const formData = new FormData();
        formData.append('archivo', archivo);
        await api.post(`/transacciones/${idTransaccion}/comprobante`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
      }

      onGuardado();
    } catch (err) {
      setError(err.response?.data || 'No se pudo guardar la transacción');
    } finally {
      setCargando(false);
    }
  };

  return (
    <div style={{ border: '1px solid #ccc', borderRadius: 8, padding: 16, marginBottom: 16 }}>
      <h3>{esEdicion ? 'Editar transacción' : 'Nueva transacción'}</h3>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 8 }}>
          <label>Tipo: </label>
          <select name="tipo" value={form.tipo} onChange={handleChange}>
            <option value="INGRESO">Ingreso</option>
            <option value="EGRESO">Egreso</option>
          </select>
        </div>

        <div style={{ marginBottom: 8 }}>
          <label>Monto: </label>
          <input type="number" step="0.01" name="monto" value={form.monto} onChange={handleChange} required />
        </div>

        <div style={{ marginBottom: 8 }}>
          <label>Fecha: </label>
          <input type="date" name="fecha" value={form.fecha} onChange={handleChange} required max={new Date().toISOString().split('T')[0]} />
        </div>

        <div style={{ marginBottom: 8 }}>
          <label>Categoría: </label>
          <select name="categoriaId" value={form.categoriaId} onChange={handleChange} required>
            <option value="">Seleccionar...</option>
            {categoriasFiltradas.map((c) => (
              <option key={c.id} value={c.id}>{c.nombre}</option>
            ))}
          </select>
        </div>

        <div style={{ marginBottom: 8 }}>
          <label>Descripción: </label>
          <input type="text" name="descripcion" value={form.descripcion} onChange={handleChange} />
        </div>

        {!esEdicion && (
          <div style={{ marginBottom: 8 }}>
            <label>
              <input type="checkbox" name="esRecurrente" checked={form.esRecurrente} onChange={handleChange} />
              {' '}Es una transacción recurrente
            </label>
          </div>
        )}

        {!esEdicion && form.esRecurrente && (
          <div style={{ marginBottom: 8, paddingLeft: 20 }}>
            <label>Frecuencia: </label>
            <select name="frecuencia" value={form.frecuencia} onChange={handleChange}>
              <option value="DIARIA">Diaria</option>
              <option value="SEMANAL">Semanal</option>
              <option value="MENSUAL">Mensual</option>
              <option value="ANUAL">Anual</option>
            </select>
            {' '}
            <label>hasta: </label>
            <input type="date" name="fechaFinRecurrencia" value={form.fechaFinRecurrencia} onChange={handleChange} required />
          </div>
        )}

        <div style={{ marginBottom: 8 }}>
          <label>Comprobante (JPG, PNG o PDF, máx. 5MB): </label>
          <input type="file" accept=".jpg,.jpeg,.png,.pdf" onChange={(e) => setArchivo(e.target.files[0])} />
        </div>

        {error && <p style={{ color: 'red' }}>{String(error)}</p>}

        <button type="submit" disabled={cargando}>{cargando ? 'Guardando...' : 'Guardar'}</button>{' '}
        <button type="button" onClick={onCancelar}>Cancelar</button>
      </form>
    </div>
  );
}