package com.msc.halisaha.entity;

public enum Rol {
    /** Platform yöneticisi: işletmelere bağlı değildir, yalnızca işletmeleri yönetir. */
    SUPER_ADMIN,
    /** İşletme sahibi/yöneticisi: kendi işletmesinin her şeyini yönetir. */
    ADMIN,
    PERSONEL
}
