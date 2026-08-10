import { useEffect, useState } from 'react';
import axios from 'axios';

function App() {
  const [mensaje, setMensaje] = useState('Conectando...');

  useEffect(() => {
    axios.get('http://localhost:8080/api/ping')
      .then(res => setMensaje(res.data))
      .catch(() => setMensaje('Error de conexión con el backend'));
  }, []);

  return (
    <div>
      <h1>FinNova</h1>
      <p>Respuesta del backend: {mensaje}</p>
    </div>
  );
}

export default App;