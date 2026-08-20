package com.example.smsexpensetracker.parser

import com.example.smsexpensetracker.parser.rules.AxisBankRule
import com.example.smsexpensetracker.parser.rules.BobBankRule
import com.example.smsexpensetracker.parser.rules.GenericBankRule
import com.example.smsexpensetracker.parser.rules.HdfcBankRule
import com.example.smsexpensetracker.parser.rules.IciciBankRule
import com.example.smsexpensetracker.parser.rules.IdfcFirstBankRule
import com.example.smsexpensetracker.parser.rules.KotakBankRule
import com.example.smsexpensetracker.parser.rules.PnbBankRule
import com.example.smsexpensetracker.parser.rules.SbiBankRule
import com.example.smsexpensetracker.parser.rules.UpiAppsRule
import com.example.smsexpensetracker.parser.rules.YesBankRule

object RuleRegistry {

    private val rules = mutableListOf<BankSmsRule>(
        HdfcBankRule(),
        IciciBankRule(),
        SbiBankRule(),
        AxisBankRule(),
        KotakBankRule(),
        PnbBankRule(),
        BobBankRule(),
        IdfcFirstBankRule(),
        YesBankRule(),
        UpiAppsRule(),
        GenericBankRule()
    )

    /**
     * Gets all registered rules ordered by descending priority.
     */
    fun getRules(): List<BankSmsRule> {
        return rules.sortedByDescending { it.priority }
    }

    /**
     * Registers a new custom bank rule dynamically.
     */
    fun registerRule(rule: BankSmsRule) {
        rules.add(rule)
    }

    /**
     * Clears and resets default rules.
     */
    fun resetDefaults() {
        rules.clear()
        rules.addAll(
            listOf(
                HdfcBankRule(),
                IciciBankRule(),
                SbiBankRule(),
                AxisBankRule(),
                KotakBankRule(),
                PnbBankRule(),
                BobBankRule(),
                IdfcFirstBankRule(),
                YesBankRule(),
                UpiAppsRule(),
                GenericBankRule()
            )
        )
    }
}
