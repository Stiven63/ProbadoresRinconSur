package com.rinconsur.probadores.model

// Estados del probador segun RN-01
enum class EstadoProbador(val texto: String) {
    LIBRE("Libre"),
    OCUPADO("Ocupado"),
    ORDENAMIENTO("En ordenamiento"),
    FUERA_SERVICIO("Fuera de servicio")
}

// Estados del turno segun RN-03
enum class EstadoTurno(val texto: String) {
    EN_ESPERA("En espera"),
    LLAMADO("Llamado"),
    ATENDIDO("Atendido"),
    ABANDONO("Abandonó"),
    NO_PRESENTO("No se presentó")
}

// Perfiles de colaborador segun RN-11
enum class Perfil(val texto: String) {
    ASISTENTE("Asistente de probadores"),
    VENDEDOR("Vendedor(a)"),
    JEFATURA("Jefatura")
}

// Motivos de abandono segun RN-07 y RN-12
// cuentaVentaPerdida = si se suma en el calculo de RN-09
enum class MotivoAbandono(val texto: String, val cuentaVentaPerdida: Boolean) {
    ESPERA_LARGA("Espera muy larga", true),
    SE_HIZO_TARDE("Se le hizo tarde", true),
    COMPRO_SIN_PROBARSE("Decidió comprar sin probarse", false),
    OTRO("Otro", true),
    NO_INFORMADO("No informado", true),
    CIERRE_TIENDA("Cierre de tienda", false);

    companion object {
        // los que puede elegir el asistente (cierre de tienda lo pone el sistema)
        fun elegibles(): List<MotivoAbandono> {
            return values().filter { it != CIERRE_TIENDA }
        }
    }
}
