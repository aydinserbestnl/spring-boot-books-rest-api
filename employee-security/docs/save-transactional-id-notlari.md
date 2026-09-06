# `addEmployee` / `save()` Serüveni: @Transactional, long vs Long, merge() vs persist()

Bu doküman, `EmployeeDAOJpalimpl.save()` metodunu çalıştırırken art arda aldığımız hataların ve bulduğumuz çözümlerin toparlanmış hâli.

---

## 1) İlk hata: `@Transactional` neden gerekti?

### Alınan hata
```
jakarta.persistence.TransactionRequiredException:
No EntityManager with actual transaction available for current thread - cannot reliably process 'merge' call
```

### Neden oluyor?
`findAll()` ve `findById()` sadece **okuma** yaptığı için transaction olmadan da çalışabiliyor. Ama `entityManager.merge(...)` bir **yazma** (INSERT/UPDATE) işlemi, ve JPA kuralı gereği her yazma işlemi aktif bir **transaction** içinde olmak zorunda. Projede hiçbir yerde (DAO, Service, Controller) transaction başlatan bir mekanizma yoktu, bu yüzden `merge()` çağrıldığı an patladı.

### Çözüm
DAO'daki yazma metotlarına (`save`, `deleteById`) `@Transactional` eklendi. Bu anotasyon Spring'e "bu metodun etrafına otomatik bir transaction aç, metot bitince commit et, hata olursa rollback et" dedirtiyor.

```java
@Override
@Transactional
public Employee save(Employee employee) {
    return entityManager.merge(employee);
}

@Override
@Transactional
public void deleteById(long id) {
    Employee employee = entityManager.find(Employee.class, id);
    entityManager.remove(employee);
}
```

> Not: `@Transactional`, `org.springframework.transaction.annotation.Transactional` paketinden import edilmeli (Jakarta'nın kendi `@Transactional`'ı değil).

---

## 2) İkinci hata: `long` (primitive) vs `Long` (wrapper) farkı

### Alınan hata
```
org.hibernate.StaleObjectStateException:
Row was already updated or deleted by another transaction for entity [...Employee with id '0']
```

### Neden oluyor?
Yeni bir çalışan eklerken `id` alanına `0` veriliyordu. Hibernate, bir `@GeneratedValue` alanının "hiç kaydedilmemiş mi" yoksa "var olan bir kayıt mı" olduğuna şu kurala göre karar veriyor (buna **unsaved-value** kontrolü denir):

| `id` alanının Java tipi | "Kaydedilmemiş" (yeni) sayılan değer | `id=0` verilirse ne olur? |
|---|---|---|
| `Long` (wrapper) | `null` | Hibernate bunu **var olan bir kayıt** sanır → veritabanında id=0 satırı yok → hata |
| `long` (primitive) | `0` (çünkü `null` olamaz) | Hibernate bunu **yeni kayıt** sayar → INSERT yapar → hata almaz |

Bu proje başta `private Long id;` kullanıyordu, bu yüzden `id=0` vermek hataya sebep oluyordu.

### Denenen iki çözüm (ikisi de gerçekten test edildi, ikisi de çalışıyor)

**Seçenek A — id tipini `long` (primitive) yap, videoyla birebir aynı kal:**
```java
@Column(name = "id")
private long id;   // Long değil, long

public long getId() { return id; }
public void setId(long id) { this.id = id; }
```
```java
// EmployeeServiceImpl.save()
public Employee save(EmployeeRequest employeeRequest) {
    return employeeDAO.save(convertToEmployee(0, employeeRequest));
}
```
Test sonucu: `POST /api/employees` → `200 OK`, `id` otomatik üretildi (örn. `7`).

**Seçenek B — id tipini `Long` (wrapper) olarak bırak, ama `0` yerine `null` ver:**
```java
public Employee save(EmployeeRequest employeeRequest) {
    Employee employee = new Employee();
    employee.setFirstName(employeeRequest.getFirstName());
    employee.setLastName(employeeRequest.getLastName());
    employee.setEmail(employeeRequest.getEmail());
    return employeeDAO.save(employee);   // id hiç set edilmedi -> null kaldı
}
```
Test sonucu: bu da `200 OK` döndü, `id` yine otomatik üretildi.

### Şu an projede hangisi aktif?
**Seçenek A** aktif (`long` primitive + `convertToEmployee(0, ...)`), çünkü video da bu şekilde ilerliyor. Seçenek B da tamamen geçerli bir alternatif; sadece "id bilinmiyor" ile "id=0" durumlarını ayırt etmek istenirse (örneğin daha büyük bir projede) `Long` + `null` yaklaşımı daha "temiz" bir sinyal sayılır.

---

## 3) `merge()` ile `persist()` arasındaki fark

İkisi de `EntityManager`'ın veritabanına yazma yapan metotları, ama farklı senaryolar için tasarlanmışlar:

| | `entityManager.persist(entity)` | `entityManager.merge(entity)` |
|---|---|---|
| Ne zaman kullanılır | Entity **kesinlikle yeni**, hiç veritabanına gitmemiş | Entity **yeni de olabilir, var olan (detached) bir kayıt da olabilir** — merge ikisini de kabul eder |
| Dönüş değeri | `void` (aynı nesneyi kendisi yönetilir hâle getirir) | **Yönetilen (managed) yeni bir kopya döndürür** — orijinal parametre değil, dönen değeri kullanmak gerekir |
| Var olan bir id ile çağrılırsa | Hata verebilir ("detached entity passed to persist") | Önce veritabanında o id'yi arar, bulursa UPDATE yapar |
| id `null`/`0` (unsaved-value) ise | INSERT yapar | Hibernate bunu transient sayıp INSERT yapar (persist gibi davranır) |
| Bizim projede kullanılan | Kullanılmıyor | `EmployeeDAOJpalimpl.save()` içinde kullanılıyor |

Bizim DAO'muz `save()` içinde her zaman `merge()` çağırıyor — hem yeni ekleme hem güncelleme için aynı metodu kullanıyoruz. Bunun çalışabilmesi için Hibernate'in az önceki tabloda anlatılan "unsaved-value" kuralına güveniyoruz (id 0/null ise yeni, doluysa güncelleme).

---

## 4) Bu konudan bağımsız: Spring Data JPA'nın kendi `save()` metodu nasıl çalışıyor?

Bu proje `EntityManager`'ı elle kullanan bir DAO yazıyor. Ama ileride (ya da videonun ilerleyen bölümlerinde) `JpaRepository<Employee, Long>` gibi hazır bir Spring Data JPA repository'si kullanılırsa, `save()` metodu **kendi başına daha akıllı bir karar mekanizmasına** sahip. Bunu bilmek, "neden orada `id=0` ile de sorunsuz çalışıyor" sorusunu cevaplıyor.

### `SimpleJpaRepository.save()` adım adım ne yapar

1. Kendisine verilen entity için `isNew(entity)` kontrolü çalıştırır.
2. `isNew()` şu sırayla karar verir:
   - Entity `Persistable` interface'ini uyguluyorsa, onun kendi `isNew()` metodunu kullanır.
   - Entity'de `@Version` alanı varsa, versiyon değerine bakar (`null`/`0` ise yeni).
   - Hiçbiri yoksa, `@Id` alanının değerine bakar — burada da **primitive/wrapper farkı** devreye girer (bir önceki bölümdeki tablo ile birebir aynı kural).
3. `isNew(entity) == true` ise → `entityManager.persist(entity)` çağırır (saf INSERT).
4. `isNew(entity) == false` ise → `entityManager.merge(entity)` çağırır (var olanı bul, güncelle).
5. Sonucu (persist edilen ya da merge'den dönen managed nesneyi) çağırana geri döndürür.

### Özet akış tablosu

| Adım | Spring Data JPA `save()` içinde olan | Bizim elle yazdığımız DAO'da olan |
|---|---|---|
| 1. Karar | `isNew()` ile yeni mi/var olan mı kontrol eder | Kontrol yok, doğrudan `merge()` çağrılır |
| 2. Yeni kayıt | `persist()` çağrılır | `merge()` çağrılır (Hibernate'in kendi unsaved-value kuralına güvenilir) |
| 3. Var olan kayıt | `merge()` çağrılır | `merge()` çağrılır (aynı metot, farklı dallanma yok) |
| 4. `id` tipi hassasiyeti | `isNew()` içinde önemli | Doğrudan Hibernate'in `merge()` içindeki unsaved-value kuralı içinde önemli |
| 5. Sonuç | Yönetilen (managed), `id`'si dolu entity döner | Aynı şekilde `id`'si dolu entity döner |

**Kısacası:** Spring Data JPA'nın `save()`'i, bizim elle yazdığımız `merge()`-only DAO'ya göre bir katman daha akıllı (önce `isNew()` ile karar veriyor), ama en temeldeki "id tipi neye göre yeni/var olan sayılıyor" kuralı ikisinde de aynı yerden geliyor: **Hibernate'in generated-id alanları için unsaved-value stratejisi.**

---

## Sonuç

- Yazma işlemi yapan DAO metotlarına (`save`, `deleteById`) mutlaka `@Transactional` ekle.
- Yeni kayıt eklerken `@GeneratedValue` alanına `Long` kullanıyorsan `null`, `long` (primitive) kullanıyorsan `0` bırak — ikisi de "bu yeni bir kayıt" anlamına geliyor, karışık kullanma.
- `persist()` sadece kesin yeni kayıtlar için, `merge()` hem yeni hem var olan kayıtlar için kullanılabilir (Hibernate id'ye bakarak kendi karar verir).
- Spring Data JPA'nın `save()`'i bizim `merge()`'ümüzün üstüne `isNew()` kontrolü koyan, biraz daha akıllı bir sürüm — ama temel mantık (id tipi/değeri üzerinden yeni/var olan ayrımı) aynı.
