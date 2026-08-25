import { api } from '../api/client';
import AssetManager from '../components/AssetManager';

export default function WalletsPage() {
  return (
    <AssetManager
      title="Crypto novčanici"
      codeLabel="Kripto valuta"
      codePlaceholder="BTC"
      defaultCode="BTC"
      codeField="cryptoCode"
      decimals={8}
      service={{
        listAll: api.listWallets,
        mine: api.myWallet,
        byEmail: api.walletOf,
        create: api.createWallet,
        update: api.updateWallet,
        remove: api.deleteWallet,
      }}
    />
  );
}
