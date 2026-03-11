document.addEventListener('DOMContentLoaded', () => {
    // CAMBIO IMPORTANTE: Usamos ruta relativa para que funcione en cualquier dominio/puerto
    const apiUrl = '/api/entregas';
    let allDeliveries = [];

    const STATUS_MAP = {
        'PENDIENTE': { class: 'status-PENDIENTE', label: 'En Espera',  icon: '⏳', priority: 1 },
        'ENVIADO':   { class: 'status-ENVIADO',   label: 'En Camino',  icon: '🚚', priority: 2 },
        'ENTREGADO': { class: 'status-ENTREGADO', label: 'Entregado',  icon: '✅', priority: 3 },
        'CANCELADO': { class: 'status-CANCELADO', label: 'Cancelado',  icon: '❌', priority: 4 }
    };

    // UI Elements
    const deliveriesGrid = document.getElementById('deliveries-grid');
    const deliveryForm = document.getElementById('delivery-form');
    const modal = document.getElementById('delivery-modal');
    const backdrop = document.getElementById('modal-backdrop');
    const drawer = document.getElementById('history-drawer');
    const drawerBackdrop = document.getElementById('drawer-backdrop');
    const searchInput = document.getElementById('search-input');
    const resetSearchBtn = document.getElementById('reset-search-btn');
    const mapModal = document.getElementById('map-modal');
    const closeMapBtn = document.getElementById('close-map-btn');
    let map = null;
    let marker = null;

    const invoicesModal = document.getElementById('invoices-modal');
    const invoicesContent = document.getElementById('invoices-content');
    const externalInvoicesBtn = document.getElementById('external-invoices-btn');
    const closeInvoicesBtn = document.getElementById('close-invoices-modal-btn');

    // --- FUNCIONES FACTURAS EXTERNAS ---
    const fetchExternalInvoices = async () => {
        invoicesContent.innerHTML = '<div class="loader">Cargando facturas desde el sistema externo...</div>';
        try {
            const response = await fetch(`${apiUrl}/facturas-externas`);
            if (!response.ok) throw new Error('Error al obtener facturas');
            const data = await response.json();
            renderInvoices(data);
        } catch (e) {
            invoicesContent.innerHTML = `<div class="error-msg">❌ No se pudo conectar con el microservicio de facturación: ${e.message}</div>`;
        }
    };

    const renderInvoices = (invoices) => {
        if (!invoices || invoices.length === 0) {
            invoicesContent.innerHTML = '<p class="no-data">No hay facturas registradas en el sistema externo.</p>';
            return;
        }

        let html = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Orden</th>
                        <th>Cliente</th>
                        <th>Total</th>
                        <th>Fecha</th>
                        <th>Estado</th>
                        <th>Acción</th>
                    </tr>
                </thead>
                <tbody>
        `;

        const assignedOrderIds = new Set(allDeliveries.map(d => parseInt(d.orderId)));

        invoices.forEach(inv => {
            const clienteNombre = inv.cliente ? 
                `${inv.cliente.nombre || ''} ${inv.cliente.apellido || ''}`.trim() : 
                'Desconocido';
            
            const isAssigned = assignedOrderIds.has(parseInt(inv.id));
            
            html += `
                <tr class="${isAssigned ? 'row-assigned' : ''}">
                    <td><strong>#${inv.id}</strong></td>
                    <td>
                        <div style="font-weight:600;">${clienteNombre}</div>
                        <div style="font-size:11px; color:#666;">${inv.cliente?.email || ''}</div>
                    </td>
                    <td style="font-weight:700; color:#2c3e50;">$${inv.total?.toFixed(2) || '0.00'}</td>
                    <td><div style="font-size:12px;">${inv.fecha?.split('T')[0] || 'N/A'}</div></td>
                    <td>
                        ${isAssigned ? 
                            '<span class="status-badge" style="background:#64748b; opacity:0.7;">📦 ASIGNADA</span>' : 
                            `<span class="status-badge status-${inv.estado}">${inv.estado || 'PAGADO'}</span>`
                        }
                    </td>
                    <td>
                        ${isAssigned ? 
                            '<button class="button" disabled style="padding: 6px 12px; font-size: 12px; background:#e2e8f0; color:#94a3b8; cursor:not-allowed;">Listo</button>' : 
                            `<button class="button primary" style="padding: 6px 12px; font-size: 12px;" 
                                     onclick="window.importInvoice(${inv.id})">
                                Gestionar
                            </button>`
                        }
                    </td>
                </tr>
            `;
        });

        html += '</tbody></table>';
        invoicesContent.innerHTML = html;
    };

    // Exponer función globalmente para los botones en el HTML generado por JS
    window.importInvoice = async (id) => {
        invoicesModal.classList.add('hidden');
        document.getElementById('order-id').value = id;
        deliveryForm.reset();
        document.getElementById('order-id').value = id;
        modal.classList.remove('hidden');
        await fetchOrderData();
    };

    externalInvoicesBtn.onclick = () => {
        invoicesModal.classList.remove('hidden');
        backdrop.classList.remove('hidden');
        fetchExternalInvoices();
    };

    closeInvoicesBtn.onclick = () => {
        invoicesModal.classList.add('hidden');
        backdrop.classList.add('hidden');
    };

    // --- NOTIFICACIONES TOAST ---
    const showToast = (message, type = 'success') => {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span>${type === 'success' ? '✅' : '❌'}</span> ${message}`;
        container.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 500);
        }, 5000);
    };

    const sortDeliveries = (list) => {
        return list.sort((a, b) => {
            const priorityA = STATUS_MAP[a.status].priority;
            const priorityB = STATUS_MAP[b.status].priority;
            if (priorityA !== priorityB) return priorityA - priorityB;
            return b.id - a.id;
        });
    };

    const performSearch = () => {
        const term = searchInput.value.trim().toLowerCase();
        if (term === "") {
            renderDeliveries(allDeliveries);
            resetSearchBtn.style.display = 'none';
            return;
        }
        const filtered = allDeliveries.filter(d =>
            d.orderId?.toString().includes(term) ||
            d.address?.toLowerCase().includes(term) ||
            d.email?.toLowerCase().includes(term) ||
            d.trackingNumber?.toLowerCase().includes(term)
        );
        renderDeliveries(filtered);
        resetSearchBtn.style.display = 'inline-block';
    };

    searchInput.oninput = performSearch;

    // --- API FUNCTIONS ---
    const fetchStats = async () => {
        try {
            const response = await fetch(`${apiUrl}/estadisticas`);
            const stats = await response.json();
            document.getElementById('count-pendiente').textContent = stats.PENDIENTE || 0;
            document.getElementById('count-enviado').textContent = stats.ENVIADO || 0;
            document.getElementById('count-entregado').textContent = stats.ENTREGADO || 0;
            document.getElementById('count-cancelado').textContent = stats.CANCELADO || 0;
        } catch (e) { console.error(e); }
    };

    const fetchDeliveries = async () => {
        try {
            const response = await fetch(apiUrl);
            allDeliveries = await response.json();
            renderDeliveries(allDeliveries);
        } catch (e) { showToast('Error de conexión con el servidor', 'error'); }
    };

    const saveDelivery = async (delivery) => {
        const isEdit = !!delivery.id;
        const method = isEdit ? 'PUT' : 'POST';
        const url = isEdit ? `${apiUrl}/${delivery.id}` : apiUrl;

        try {
            const response = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(delivery)
            });

            if (response.ok) {
                showToast(isEdit ? 'Actualizado correctamente' : 'Creado con éxito');
                closeModal();
                fetchDeliveries();
                fetchStats();
            } else {
                const errorData = await response.json();
                const mensajeLimpio = errorData.message || 'Error desconocido al guardar';
                showToast(mensajeLimpio, 'error');
            }
        } catch (e) {
            showToast('Error crítico de red o servidor', 'error');
        }
    };

    const downloadPdf = (id) => {
        window.location.href = `${apiUrl}/${id}/pdf`;
    };

    const showOnMap = (address) => {
        mapModal.classList.remove('hidden');
        document.getElementById('map-address-text').textContent = "Buscando: " + address;
        
        if (!map) {
            map = L.map('map').setView([-1.024, -79.46], 13); // UTEQ Quevedo por defecto
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            }).addTo(map);
        }

        // Simulación de Geocodificación (en un entorno real usaríamos Nominatim)
        // Para fines demostrativos, ponemos un punto aleatorio cerca de Quevedo basado en el texto
        setTimeout(() => {
            const lat = -1.02 + (Math.random() * 0.02);
            const lng = -79.46 + (Math.random() * 0.02);
            const pos = [lat, lng];
            
            if (marker) marker.remove();
            marker = L.marker(pos).addTo(map)
                .bindPopup(`<b>Destino:</b><br>${address}`)
                .openPopup();
            
            map.setView(pos, 15);
            map.invalidateSize();
        }, 500);
    };

    const deleteDelivery = async (id) => {
        if (!confirm('¿Eliminar esta entrega permanentemente?')) return;
        try {
            const response = await fetch(`${apiUrl}/${id}`, { method: 'DELETE' });
            if (response.ok) {
                showToast('Entrega eliminada');
                fetchDeliveries();
                fetchStats();
            }
        } catch (e) { showToast('Error al eliminar', 'error'); }
    };

    const renderDeliveries = (deliveries) => {
        deliveriesGrid.innerHTML = '';
        const sorted = sortDeliveries([...deliveries]);

        if (sorted.length === 0) {
            deliveriesGrid.innerHTML = '<p style="grid-column:1/-1; text-align:center; color:#999; margin-top:20px;">No hay resultados.</p>';
            return;
        }

        sorted.forEach(d => {
            const config = STATUS_MAP[d.status];
            const card = document.createElement('div');
            card.className = 'delivery-card';
            card.innerHTML = `
                <div class="card-header">
                    <h3>Orden #${d.orderId}</h3>
                    <span class="status-badge ${config.class}">${config.icon} ${config.label}</span>
                </div>
                <div class="card-body">
                    <p><strong>👤 Cliente:</strong> ${d.customerName || 'Inquilino'}</p>
                    <p><strong>📍 Dirección:</strong> ${d.address}</p>
                    <p><strong>📧 Email:</strong> ${d.email || 'N/A'}</p>
                    <p><strong>📦 Seguimiento:</strong> ${d.trackingNumber || 'N/A'}</p>
                </div>
                <div class="card-footer" style="flex-wrap: wrap; gap: 4px;">
                    <button class="card-button edit-btn" style="flex: 1;">Editar</button>
                    <button class="card-button pdf-btn" style="flex: 1; background: #e74c3c; color: white;">📄 PDF</button>
                    <button class="card-button map-btn" style="flex: 1; background: #27ae60; color: white;">📍 Mapa</button>
                    <button class="card-button history-btn" style="flex: 1;">📜 Historial</button>
                    <button class="card-button delete-btn" style="flex: 1; background: #eee; color: #666;">Eliminar</button>
                </div>
            `;
            deliveriesGrid.appendChild(card);
            card.querySelector('.edit-btn').onclick = () => openModalForEdit(d);
            card.querySelector('.pdf-btn').onclick = () => downloadPdf(d.id);
            card.querySelector('.map-btn').onclick = () => showOnMap(d.address);
            card.querySelector('.delete-btn').onclick = () => deleteDelivery(d.id);
            card.querySelector('.history-btn').onclick = () => openHistory(d);
        });
    };

    const openHistory = (d) => {
        const content = document.getElementById('drawer-content');
        content.innerHTML = `
            <div class="drawer-info-box">
                <p><strong>Orden:</strong> #${d.orderId}</p>
                <p><strong>Cliente:</strong> ${d.email}</p>
                <p><strong>Destino:</strong> ${d.address}</p>
            </div>
            <div class="timeline">
                <div class="timeline-item"><h4>Orden Registrada</h4><p>ID de Orden #${d.orderId} validado en sistema.</p></div>
                <div class="timeline-item"><h4>Estado Actual: ${d.status}</h4><p>Notificación enviada al correo del cliente.</p></div>
            </div>
        `;
        drawer.classList.remove('hidden');
        drawerBackdrop.classList.remove('hidden');
    };

    document.querySelectorAll('.stat-card').forEach(card => {
        card.onclick = () => {
            const statusKey = card.id.split('-')[1].toUpperCase();
            const filtered = allDeliveries.filter(d => d.status === statusKey);
            renderDeliveries(filtered);
            resetSearchBtn.style.display = 'inline-block';
            showToast(`Filtrando: ${statusKey}`);
        };
    });

    const openModalForEdit = (d) => {
        document.getElementById('modal-title').textContent = 'Editar Entrega';
        document.getElementById('delivery-id').value = d.id;
        document.getElementById('order-id').value = d.orderId;
        document.getElementById('customer-name').value = d.customerName || '';
        document.getElementById('address').value = d.address;
        document.getElementById('email').value = d.email || '';
        document.getElementById('tracking-number').value = d.trackingNumber || '';
        document.getElementById('status').value = d.status;
        modal.classList.remove('hidden');
        backdrop.classList.remove('hidden');
    };

    const closeModal = () => {
        modal.classList.add('hidden');
        backdrop.classList.add('hidden');
        drawer.classList.add('hidden');
        drawerBackdrop.classList.add('hidden');
    };

    document.getElementById('add-delivery-btn').onclick = () => {
        document.getElementById('modal-title').textContent = 'Agregar Entrega';
        deliveryForm.reset();
        document.getElementById('delivery-id').value = '';
        document.getElementById('customer-name').value = '';
        modal.classList.remove('hidden');
        backdrop.classList.remove('hidden');
    };

    document.getElementById('close-modal-btn').onclick = closeModal;
    document.getElementById('close-drawer-btn').onclick = closeModal;
    closeMapBtn.onclick = () => { mapModal.classList.add('hidden'); };
    backdrop.onclick = closeModal;
    drawerBackdrop.onclick = closeModal;

    const fetchOrderData = async () => {
        const orderId = document.getElementById('order-id').value;
        if (!orderId) {
            showToast('Ingresa un ID de Orden primero', 'error');
            return;
        }

        const btn = document.getElementById('btn-fetch-order');
        const originalText = btn.innerHTML;
        btn.innerHTML = '⌛...';
        btn.disabled = true;

        try {
            const response = await fetch(`${apiUrl}/preparar/${orderId}`);
            if (response.ok) {
                const data = await response.json();
                document.getElementById('customer-name').value = data.customerName || '';
                document.getElementById('address').value = data.address || '';
                document.getElementById('email').value = data.email || '';
                document.getElementById('status').value = data.status || 'PENDIENTE';
                showToast('Datos de orden recuperados');
            } else {
                showToast('Orden no encontrada en registros externos', 'error');
            }
        } catch (e) {
            showToast('Error al conectar con el servidor', 'error');
        } finally {
            btn.innerHTML = originalText;
            btn.disabled = false;
        }
    };

    document.getElementById('btn-fetch-order').onclick = fetchOrderData;

    deliveryForm.onsubmit = (e) => {
        e.preventDefault();
        const data = {
            orderId: parseInt(document.getElementById('order-id').value),
            customerName: document.getElementById('customer-name').value,
            address: document.getElementById('address').value,
            email: document.getElementById('email').value,
            trackingNumber: document.getElementById('tracking-number').value,
            status: document.getElementById('status').value
        };
        const id = document.getElementById('delivery-id').value;
        if (id) data.id = parseInt(id);
        saveDelivery(data);
    };

    resetSearchBtn.onclick = () => {
        searchInput.value = '';
        renderDeliveries(allDeliveries);
        resetSearchBtn.style.display = 'none';
    };

    fetchDeliveries();
    fetchStats();
});