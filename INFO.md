# Reiniciar DB
cd needlos-backend
docker compose down -v    # borra el contenedor Y el volumen (todos los datos)
docker compose up -d      # crea una base vacía, nueva

#Datos prueba
Correo	Contraseña	Rol	Sastrerías
pablys8@gmail.com	— (solo Google, sin contraseña)	SUPER_ADMIN	Ninguna (ve el panel global)
admin@example.com	Test123!	SASTRE_ADMIN	SastreriaPablo y SastreriaAngely (verás el selector)
sastre@example.com	Test123!	SASTRE (empleado)	SastreriaPablo y SastreriaAngely (verás el selector)
Dos sastrerías de prueba: SastreriaPablo y SastreriaAngely, cada una con un catálogo básico de tipos de prenda.