package com.anlian.italase.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anlian.italase.shared.data.local.Tables
import com.anlian.italase.shared.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/** Akses tabel products. Semua Flow di sini reaktif terhadap perubahan tabel,
 *  jadi pemanggil tidak perlu — dan tidak boleh — menyegarkan datanya sendiri. */
@Dao
interface ProductDao {
    /** Semua produk urut nama. Dipanggil lewat ShopRepository yang men-cache
     *  hasilnya, jangan panggil langsung dari layar. */
    @Query("SELECT * FROM ${Tables.PRODUCTS} ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    /** Null berarti produk sudah dihapus di antara pemuatan daftar dan aksi ini. */
    @Query("SELECT * FROM ${Tables.PRODUCTS} WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    /** Barang yang stock-nya sudah menyentuh minStock-nya sendiri; ambang
     *  cadangan untuk minStock 0 diterapkan di UI, bukan di SQL ini. */
    @Query("SELECT * FROM ${Tables.PRODUCTS} WHERE stock <= minStock ORDER BY stock ASC")
    fun observeLowStock(): Flow<List<ProductEntity>>

    /** id 0 berarti baris baru, selain itu menimpa baris dengan id tersebut.
     *  Mengembalikan id baris yang tersimpan. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProductEntity): Long

    /** Menimpa baris dengan id yang sama; tidak pernah membuat baris baru. */
    @Update
    suspend fun update(item: ProductEntity)

    /** Hapus permanen. Riwayat struk tidak ikut terhapus karena sale_items
     *  menyimpan salinan namanya. */
    @Delete
    suspend fun delete(item: ProductEntity)

    /** Jumlah baris products; untuk membedakan toko yang datanya masih kosong. */
    @Query("SELECT COUNT(*) FROM ${Tables.PRODUCTS}")
    suspend fun count(): Int
}
