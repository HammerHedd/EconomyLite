/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2017 Flibio
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.flibio.economylite.modules.loan.command;

import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.modules.loan.LoanModule;
import io.github.flibio.economylite.modules.loan.LoanTextUtils;
import io.github.flibio.utils.commands.AsyncCommand;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.commands.ParentCommand;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.text.Text;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@AsyncCommand
@ParentCommand(parentCommand = LoanCommand.class)
@Command(aliases = {"take"}, permission = "economylite.loan.take")
public class LoanTakeCommand extends BaseCommandExecutor<Player> {

    private MessageStorage messages = EconomyLite.getMessageStorage();
    private LoanModule module;

    public LoanTakeCommand(LoanModule module) {
        this.module = module;
    }

    @Override
    public Builder getCommandSpecBuilder() {
        return CommandSpec.builder()
                .arguments(GenericArguments.doubleNum(Text.of("amount")))
                .executor(this);
    }

    @Override
    public void run(Player src, CommandContext args) {
        if (args.getOne("amount").isPresent()) {
            UUID uuid = src.getUniqueId();
            double loanAmount = args.<Double>getOne("amount").get();
            Currency cur = EconomyLite.getEconomyService().getDefaultCurrency();
            // Get player balance
            Optional<Double> dOpt = module.getLoanManager().getLoanBalance(uuid);
            if (dOpt.isPresent()) {
                double loanBalance = dOpt.get();
                // Notify player of interest rate
                src.sendMessage(messages.getMessage("module.loan.interest", "rate", Text.of(module.getInterestRate())));
                // Check how much loan they can take out
                double maxLoan = (module.getMaxLoan() - loanBalance) / module.getInterestRate();
                if (maxLoan <= 0) {
                    // Tell player they are out of loan balance
                    src.sendMessage(messages.getMessage("modules.loan.full"));
                }
                if (maxLoan < loanAmount) {
                    // Offer the player a smaller loan
                    src.sendMessage(messages.getMessage("module.loan.partial"));
                    src.sendMessage(messages.getMessage("module.loan.ask", "amount",
                            Text.of(String.format(Locale.ENGLISH, "%,.2f", maxLoan)), "label", getPrefix(maxLoan, cur)));
                    double total = maxLoan * module.getInterestRate();
                    src.sendMessage(messages.getMessage("module.loan.payment", "amount",
                            Text.of(String.format(Locale.ENGLISH, "%,.2f", total)), "label", getPrefix(total, cur)));
                    module.tableLoans.remove(uuid);
                    module.tableLoans.put(uuid, maxLoan);
                } else {
                    // Ask the player if they want a full loan
                    src.sendMessage(messages.getMessage("module.loan.ask", "amount", Text.of(String.format(Locale.ENGLISH, "%,.2f", loanAmount)),
                            "label", getPrefix(loanAmount, cur)));
                    double total = loanAmount * module.getInterestRate();
                    src.sendMessage(messages.getMessage("module.loan.payment", "amount",
                            Text.of(String.format(Locale.ENGLISH, "%,.2f", total)), "label", getPrefix(total, cur)));

                    src.sendMessage(LoanTextUtils.yesOrNo("/loan accept", "/loan deny"));
                    module.tableLoans.remove(uuid);
                    module.tableLoans.put(uuid, loanAmount);
                }
            } else {
                src.sendMessage(messages.getMessage("command.error"));
            }
        } else {
            src.sendMessage(messages.getMessage("command.error"));
        }
    }

    private Text getPrefix(double amnt, Currency cur) {
        Text label = cur.getPluralDisplayName();
        if (amnt == 1.0) {
            label = cur.getDisplayName();
        }
        return label;
    }

}
