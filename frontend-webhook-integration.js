// Actualización del paymentsService con integración de webhooks
const API_URL = "http://localhost:8080/api/mercadopago";

export const paymentsService = {
    // Métodos existentes
    getStatus: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/oauth/status`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo obtener el estado de Mercado Pago");
        const data = await response.json();
        return data.data; // Extraer data del ApiResponse
    },

    disconnect: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/oauth/disconnect`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo desvincular Mercado Pago");
        return true;
    },

    createPaymentPreference: async (paymentData) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(paymentData)
        });
        if (!response.ok) throw new Error("No se pudo crear la preferencia de pago");
        const data = await response.json();
        return data.data; // Extraer data del ApiResponse
    },

    // Nuevos métodos para webhooks y estado en tiempo real
    getUserPreferences: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudieron obtener las preferencias");
        const data = await response.json();
        return data.data;
    },

    getPreferenceById: async (preferenceId) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences/${preferenceId}`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo obtener la preferencia");
        const data = await response.json();
        return data.data;
    },

    getPendingPayments: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/appointments/pending-payment`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudieron obtener los pagos pendientes");
        const data = await response.json();
        return data.data;
    },

    cancelPreference: async (preferenceId) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences/${preferenceId}/cancel`, {
            method: "POST",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo cancelar la preferencia");
        return true;
    },

    getPaymentStatus: async (paymentId) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/payment/${paymentId}/status`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo obtener el estado del pago");
        const data = await response.json();
        return data.data;
    },

    getStatistics: async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/statistics`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudieron obtener las estadísticas");
        const data = await response.json();
        return data.data;
    },

    // Método para verificar si una preferencia tiene reserva creada (webhook procesado)
    checkAppointmentCreated: async (preferenceId) => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_URL}/preferences/${preferenceId}/appointment-status`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
        if (!response.ok) throw new Error("No se pudo verificar el estado de la reserva");
        const data = await response.json();
        return data.data;
    }
};

// Hook personalizado para monitorear el estado de pagos en tiempo real
export const usePaymentWebhook = (preferenceId, interval = 3000) => {
    const [paymentStatus, setPaymentStatus] = useState(null);
    const [appointmentCreated, setAppointmentCreated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!preferenceId) return;

        const checkPaymentStatus = async () => {
            try {
                // Obtener estado de la preferencia
                const preference = await paymentsService.getPreferenceById(preferenceId);
                setPaymentStatus(preference.status);

                // Si el pago fue aprobado, verificar si se creó la reserva
                if (preference.status === 'approved') {
                    try {
                        const appointmentStatus = await paymentsService.checkAppointmentCreated(preferenceId);
                        setAppointmentCreated(appointmentStatus.appointmentCreated);
                    } catch (err) {
                        console.log('Reserva aún no creada');
                    }
                }

                setError(null);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        // Verificar inmediatamente
        checkPaymentStatus();

        // Configurar polling para simular webhooks en tiempo real
        const intervalId = setInterval(checkPaymentStatus, interval);

        return () => clearInterval(intervalId);
    }, [preferenceId, interval]);

    return { paymentStatus, appointmentCreated, loading, error };
};

// Hook para monitorear múltiples preferencias
export const useMultiplePaymentWebhooks = (preferenceIds, interval = 5000) => {
    const [payments, setPayments] = useState({});
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!preferenceIds || preferenceIds.length === 0) return;

        const checkAllPayments = async () => {
            try {
                const paymentPromises = preferenceIds.map(async (id) => {
                    try {
                        const preference = await paymentsService.getPreferenceById(id);
                        return { id, ...preference };
                    } catch (err) {
                        return { id, error: err.message };
                    }
                });

                const results = await Promise.all(paymentPromises);
                const paymentsMap = {};
                results.forEach(result => {
                    paymentsMap[result.id] = result;
                });

                setPayments(paymentsMap);
            } catch (err) {
                console.error('Error checking payments:', err);
            } finally {
                setLoading(false);
            }
        };

        checkAllPayments();
        const intervalId = setInterval(checkAllPayments, interval);

        return () => clearInterval(intervalId);
    }, [preferenceIds, interval]);

    return { payments, loading };
}; 