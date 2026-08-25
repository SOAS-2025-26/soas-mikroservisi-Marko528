import { api } from '../api/client';
import AssetManager from '../components/AssetManager';

export default function BankAccountsPage() {
  return (
    <AssetManager
      title="Bankovni računi"
      codeLabel="Valuta"
      codePlaceholder="EUR"
      defaultCode="EUR"
      codeField="currencyCode"
      decimals={2}
      service={{
        listAll: api.listBankAccounts,
        mine: api.myBankAccount,
        byEmail: api.bankAccountOf,
        create: api.createBankAccount,
        update: api.updateBankAccount,
        remove: api.deleteBankAccount,
      }}
    />
  );
}
