package com.itangcent.intellij.psi

import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.itangcent.intellij.spring.MultipartFile
import com.itangcent.intellij.util.KV
import com.sun.jmx.remote.internal.ArrayQueue
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

interface PsiClassHelper {
    fun getTypeObject(psiType: PsiType?, context: PsiElement): Any?

    fun getTypeObject(psiType: PsiType?, context: PsiElement, option: Int): Any?

    fun getFields(psiClass: PsiClass?): KV<String, Any?>

    fun getFields(psiClass: PsiClass?, option: Int): KV<String, Any?>

    fun isNormalType(typeName: String): Boolean

    fun copy(obj: Any?): Any?

    fun unboxArrayOrList(psiType: PsiType): PsiType

    fun getDefaultValue(typeName: String): Any?
    
    fun resolvePropertyOrMethodOfClass(psiClass: PsiClass, propertyOrMethod: String): PsiElement?

    fun getJsonFieldName(psiField: PsiField): String

    fun parseStaticFields(psiClass: PsiClass): List<Map<String, Any?>>

    fun parseEnumConstant(psiClass: PsiClass): List<Map<String, Any?>>

    fun getAttrOfField(field: PsiField): String?

    fun resolveEnumOrStatic(
        classNameWithProperty: String,
        psiMember: PsiMember,
        defaultPropertyName: String
    ): ArrayList<HashMap<String, Any?>>?

    fun resolveClass(className: String, psiMember: PsiMember): PsiClass?

    fun getContainingClass(psiMember: PsiMember): PsiClass?

    companion object {

        val JAVA_OBJECT_METHODS: Array<String> = arrayOf(
            "registerNatives",
            "getClass",
            "hashCode",
            "equals",
            "clone",
            "toString",
            "notify",
            "notifyAll",
            "wait",
            "finalize"
        )

        val ELEMENT_OF_COLLECTION = "E"
        val KEY_OF_MAP = "K"
        val VALUE_OF_MAP = "V"

        fun isCollection(psiType: PsiType): Boolean {
            if (collectionClasses!!.contains(psiType.presentableText)) {
                return true
            }

            val cls = PsiUtil.resolveClassInType(psiType)
            if (cls != null) {
                for (superCls in cls.supers) {
                    if (collectionClasses!!.contains(superCls.qualifiedName)) {
                        return true
                    }
                }
            }

            return false
        }

        fun isMap(psiType: PsiType): Boolean {
            if (mapClasses!!.contains(psiType.presentableText)) {
                return true
            }

            val cls = PsiUtil.resolveClassInType(psiType)
            if (cls != null) {
                if (mapClasses!!.contains(cls.qualifiedName)) {
                    return true
                }
                for (superCls in cls.supers) {
                    if (mapClasses!!.contains(superCls.qualifiedName)) {
                        return true
                    }
                }
            }

            return false
        }

        //represent spring MultipartFile
        val multipartFileInstance = MultipartFile()
//        val multipartFileInstance = "file"

        var fieldModifiers: Set<String> = HashSet(Arrays.asList(PsiModifier.PRIVATE, PsiModifier.PROTECTED))
        var staticFinalFieldModifiers: Set<String> =
            HashSet(Arrays.asList(PsiModifier.STATIC, PsiModifier.FINAL))
        var publicStaticFinalFieldModifiers: Set<String> = HashSet(
            Arrays.asList(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)
        )

        val normalTypes: HashMap<String, Any?> = HashMap()

        var collectionClasses: Set<String>? = null
        var mapClasses: Set<String>? = null
        var castToString: Set<String>? = null
        fun init() {
            if (normalTypes.isEmpty()) {
                normalTypes["Boolean"] = false
                normalTypes["Void"] = null
                normalTypes["Byte"] = 0
                normalTypes["Short"] = 0
                normalTypes["Integer"] = 0
                normalTypes["Long"] = 0L
                normalTypes["Float"] = 0.0F
                normalTypes["Double"] = 0.0
                normalTypes["String"] = ""
                normalTypes["BigDecimal"] = 0.0
                normalTypes["Class"] = null
                normalTypes["java.lang.Boolean"] = false
                normalTypes["java.lang.Void"] = null
                normalTypes["java.lang.Byte"] = 0
                normalTypes["java.lang.Short"] = 0
                normalTypes["java.lang.Integer"] = 0
                normalTypes["java.lang.Long"] = 0L
                normalTypes["java.lang.Float"] = 0.0F
                normalTypes["java.lang.Double"] = 0.0
                normalTypes["java.lang.String"] = ""
                normalTypes["java.math.BigDecimal"] = 0.0
                normalTypes["java.lang.Class"] = null
            }
            if (collectionClasses == null) {
                val collectionClasses = HashSet<String>()
                addClass(Collection::class.java, collectionClasses)
                addClass(List::class.java, collectionClasses)
                addClass(ArrayList::class.java, collectionClasses)
                addClass(LinkedList::class.java, collectionClasses)
                addClass(Set::class.java, collectionClasses)
                addClass(HashSet::class.java, collectionClasses)
                addClass(TreeSet::class.java, collectionClasses)
                addClass(SortedSet::class.java, collectionClasses)
                addClass(Queue::class.java, collectionClasses)
                addClass(Deque::class.java, collectionClasses)
                addClass(ArrayQueue::class.java, collectionClasses)
                addClass(ArrayBlockingQueue::class.java, collectionClasses)
                addClass(Stack::class.java, collectionClasses)
                this.collectionClasses = collectionClasses
            }
            if (mapClasses == null) {
                val mapClasses = HashSet<String>()
                addClass(Map::class.java, mapClasses)
                addClass(HashMap::class.java, mapClasses)
                addClass(LinkedHashMap::class.java, mapClasses)
                this.mapClasses = mapClasses
            }
            if (castToString == null) {
                val castToString = HashSet<String>()
                addClass(Date::class.java, castToString)
                castToString.add("ObjectId")
                castToString.add("org.bson.types.ObjectId")
                this.castToString = castToString
            }
        }

        init {
            init()
        }

        fun hasAnyModify(modifierListOwner: PsiModifierListOwner, modifies: Set<String>): Boolean {
            val modifierList = modifierListOwner.modifierList ?: return false
            return modifies.any { modifierList.hasModifierProperty(it) }
        }

        private fun addClass(cls: Class<*>, classSet: HashSet<String>) {
            classSet.add(cls.name!!)
            classSet.add(cls.simpleName!!)
        }
    }
}