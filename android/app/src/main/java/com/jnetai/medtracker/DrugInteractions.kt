package com.jnetai.medtracker

/**
 * Basic built-in drug interaction database.
 * Covers common OTC and prescription medication interactions.
 */
object DrugInteractions {

    data class Interaction(
        val drug1: String,
        val drug2: String,
        val severity: Severity,
        val description: String
    )

    enum class Severity { LOW, MODERATE, HIGH }

    private val interactions = listOf(
        // NSAID interactions
        Interaction("ibuprofen", "aspirin", Severity.HIGH, "Both are NSAIDs — combined use increases risk of gastrointestinal bleeding and ulcers."),
        Interaction("ibuprofen", "warfarin", Severity.HIGH, "Ibuprofen increases bleeding risk when taken with warfarin. Avoid combination."),
        Interaction("aspirin", "warfarin", Severity.HIGH, "Aspirin increases anticoagulant effect of warfarin — significantly increased bleeding risk."),
        Interaction("naproxen", "warfarin", Severity.HIGH, "Naproxen increases bleeding risk with warfarin. Avoid combination."),

        // SSRI interactions
        Interaction("sertraline", "ibuprofen", Severity.MODERATE, "SSRIs + NSAIDs increase risk of bleeding, especially GI bleeding."),
        Interaction("fluoxetine", "ibuprofen", Severity.MODERATE, "SSRIs + NSAIDs increase risk of bleeding."),
        Interaction("citalopram", "ibuprofen", Severity.MODERATE, "SSRIs + NSAIDs increase bleeding risk."),
        Interaction("sertraline", "aspirin", Severity.MODERATE, "SSRIs + aspirin increase risk of bleeding."),
        Interaction("fluoxetine", "aspirin", Severity.MODERATE, "SSRIs + aspirin increase bleeding risk."),
        Interaction("sertraline", "tramadol", Severity.HIGH, "Risk of serotonin syndrome when combining SSRIs with tramadol."),
        Interaction("fluoxetine", "tramadol", Severity.HIGH, "Risk of serotonin syndrome. Avoid if possible."),
        Interaction("citalopram", "tramadol", Severity.HIGH, "Risk of serotonin syndrome when combining with tramadol."),

        // MAOI interactions (dangerous)
        Interaction("phenelzine", "sertraline", Severity.HIGH, "MAOIs + SSRIs can cause fatal serotonin syndrome. Never combine."),
        Interaction("phenelzine", "fluoxetine", Severity.HIGH, "MAOIs + SSRIs can cause fatal serotonin syndrome. Never combine."),
        Interaction("selegiline", "sertraline", Severity.HIGH, "MAOIs + SSRIs risk serotonin syndrome. Do not combine."),

        // Statin interactions
        Interaction("simvastatin", "grapefruit", Severity.MODERATE, "Grapefruit increases simvastatin blood levels — increased risk of muscle damage."),
        Interaction("atorvastatin", "grapefruit", Severity.LOW, "Grapefruit may slightly increase atorvastatin levels. Moderate consumption usually OK."),

        // Blood pressure
        Interaction("lisinopril", "ibuprofen", Severity.MODERATE, "NSAIDs can reduce the blood pressure lowering effect of ACE inhibitors and may harm kidneys."),
        Interaction("amlodipine", "grapefruit", Severity.MODERATE, "Grapefruit increases amlodipine blood levels — may cause excessive blood pressure drop."),

        // Thyroid
        Interaction("levothyroxine", "calcium", Severity.MODERATE, "Calcium supplements can reduce levothyroxine absorption. Take at least 4 hours apart."),
        Interaction("levothyroxine", "iron", Severity.MODERATE, "Iron supplements reduce levothyroxine absorption. Take at least 4 hours apart."),

        // Diabetes
        Interaction("metformin", "alcohol", Severity.MODERATE, "Alcohol increases risk of lactic acidosis with metformin. Limit alcohol intake."),

        // Antibiotics
        Interaction("ciprofloxacin", "aluminium", Severity.MODERATE, "Antacids with aluminium/magnesium reduce ciprofloxacin absorption. Take 2 hours apart."),
        Interaction("ciprofloxacin", "magnesium", Severity.MODERATE, "Antacids with magnesium reduce ciprofloxacin absorption. Take 2 hours apart."),
        Interaction("doxycycline", "calcium", Severity.MODERATE, "Calcium reduces doxycycline absorption. Take 2 hours apart."),
        Interaction("doxycycline", "iron", Severity.MODERATE, "Iron reduces doxycycline absorption. Take 2 hours apart."),

        // Sedatives
        Interaction("lorazepam", "alcohol", Severity.HIGH, "Benzodiazepines + alcohol cause dangerous CNS depression. Avoid combination."),
        Interaction("diazepam", "alcohol", Severity.HIGH, "Benzodiazepines + alcohol cause dangerous CNS depression. Avoid combination."),
        Interaction("zolpidem", "alcohol", Severity.HIGH, "Sedative-hypnotics + alcohol cause dangerous CNS depression."),
        Interaction("lorazepam", "zolpidem", Severity.HIGH, "Combined use increases risk of excessive sedation and respiratory depression."),

        // Antihistamines
        Interaction("diphenhydramine", "alcohol", Severity.MODERATE, "Both cause drowsiness — combined effect is significantly increased sedation."),
        Interaction("cetirizine", "alcohol", Severity.LOW, "May increase drowsiness when combined with alcohol."),

        // Anticoagulants
        Interaction("warfarin", "vitamin k", Severity.HIGH, "Vitamin K opposes warfarin's anticoagulant effect. Keep vitamin K intake consistent."),
        Interaction("warfarin", "garlic", Severity.MODERATE, "Garlic supplements may increase bleeding risk with warfarin."),
        Interaction("warfarin", "gingko", Severity.MODERATE, "Ginkgo may increase bleeding risk with warfarin."),

        // More common
        Interaction("omeprazole", "clopidogrel", Severity.HIGH, "Omeprazole reduces the antiplatelet effect of clopidogrel. Use pantoprazole instead if needed."),
        Interaction("metformin", "contrast", Severity.MODERATE, "Stop metformin before IV contrast dye to reduce risk of lactic acidosis. Restart after 48h if kidney function normal."),
    )

    /**
     * Check for interactions between a list of medication names.
     * Returns list of found interactions.
     */
    fun checkInteractions(medNames: List<String>): List<Interaction> {
        val lowerNames = medNames.map { it.lowercase().trim() }
        val found = mutableListOf<Interaction>()
        for (i in lowerNames.indices) {
            for (j in i + 1 until lowerNames.size) {
                val name1 = lowerNames[i]
                val name2 = lowerNames[j]
                for (interaction in interactions) {
                    val matches = (name1.contains(interaction.drug1) && name2.contains(interaction.drug2)) ||
                                  (name1.contains(interaction.drug2) && name2.contains(interaction.drug1))
                    if (matches) {
                        if (found.none { it.drug1 == interaction.drug1 && it.drug2 == interaction.drug2 }) {
                            found.add(interaction.copy(
                                drug1 = medNames[i],
                                drug2 = medNames[j]
                            ))
                        }
                    }
                }
            }
        }
        return found
    }

    fun getSeverityEmoji(severity: Severity): String = when (severity) {
        Severity.LOW -> "⚠️"
        Severity.MODERATE -> "🟡"
        Severity.HIGH -> "🔴"
    }
}