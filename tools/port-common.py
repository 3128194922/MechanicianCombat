"""Apply the first reviewed 1.20.1 API conversions to the generated baseline."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/com/mechaniciancombat'
replacements={
 'Item.Settings':'Item.Properties','TooltipContext':'net.minecraft.world.level.Level',
 'HolderLookup.WrapperLookup':'net.minecraft.core.RegistryAccess',
 'RegistryFriendlyByteBuf':'FriendlyByteBuf',
 'AbstractArrow.PickupPermission':'AbstractArrow.Pickup',
 'AttributeModifier.Operation.ADD_VALUE':'AttributeModifier.Operation.ADDITION',
 'AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL':'AttributeModifier.Operation.MULTIPLY_TOTAL',
 'AttributeModifier.Operation.ADD_MULTIPLIED_BASE':'AttributeModifier.Operation.MULTIPLY_BASE',
 'DataComponents.CUSTOM_DATA':'DataComponents.CUSTOM_DATA',
 '.world':'.level','.maxCount(':'.stacksTo(','.increment(':'.grow(',
 '.lengthSquared()':'.lengthSqr()', '.squaredDistanceTo(':'.distanceToSqr(',
 '.hasInfiniteMaterials()':'.getAbilities().instabuild',
 '.sendMessage(':'.displayClientMessage(',
 '.getEquippedStack(':'.getItemBySlot(',
 '.isSameItemSameComponents(':'.isSameItemSameTags(',
 '.isPressed()':'.isDown()', '.setPressed(':'.setDown(',
 '.fromTranslationKey(':'.getKey(','.isKeyPressed(':'.isKeyDown(',
 'InputConstants.UNKNOWN_KEY':'InputConstants.UNKNOWN',
 '.createFromCode(':'.getOrCreate(',
 '.getCategory()':'.getType()',
 'BlockBehaviour.Properties.create()':'BlockBehaviour.Properties.of()',
 '.sounds(':'.sound(', 'NonNullList.ofSize(': 'NonNullList.withSize(',
 'ResourceLocation.of(': 'new ResourceLocation(',
 'ResourceLocation.ofVanilla(': 'new ResourceLocation(',
 'SoundEvent.of(': 'SoundEvent.createVariableRangeEvent(',
 '.getBodyYaw()':'.yBodyRot',
 '.getServer()':'.getServer()',
 '.readString()':'.readUtf(32767)',
}
for path in JAVA.rglob('*.java'):
    s=path.read_text(encoding='utf-8')
    # Do not alter packages like net.minecraft.world when translating client.world.
    for old,new in replacements.items():
        if old=='.world':
            s=re.sub(r'\b(client|mc|minecraft)\.world\b',lambda m:m[1]+'.level',s)
        else: s=s.replace(old,new)
    s=re.sub(r'\bSettings\b','Properties',s)
    s=re.sub(r'(BuiltInRegistries\.[A-Z_]+)\s*\.getId\(',r'\1.getKey(',s)
    s=s.replace('import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;', 'import com.mechaniciancombat.net.ModNetwork;')
    s=s.replace('ServerPlayNetworking.', 'ModNetwork.')
    s=s.replace('import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;', 'import com.mechaniciancombat.client.ClientNetwork;')
    s=s.replace('ClientPlayNetworking.', 'ClientNetwork.')
    if path.parent.name=='net':
        s=s.replace('import net.minecraft.network.codec.StreamCodec;','')
        s=s.replace('import net.minecraft.network.protocol.common.custom.CustomPacketPayload;','')
        s=s.replace('CustomPacketPayload.Id','PacketType').replace('CustomPacketPayload','ModPacket')
        s=s.replace('StreamCodec<FriendlyByteBuf, ', 'PacketCodec<').replace('StreamCodec.of(', 'PacketCodec.of(')
        s=s.replace('new PacketType<>(TkGun.id(',f'new PacketType<>({path.stem}.class, TkGun.id(')
    path.write_text(s,encoding='utf-8')
