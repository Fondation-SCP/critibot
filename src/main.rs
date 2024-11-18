use std::collections::{HashMap, HashSet};
use std::env;

use fondabots_lib::{affichan::Affichan, Bot, ErrType};
use poise::futures_util::FutureExt;
use poise::{BoxFuture, Context};
use serenity::all::{ChannelId, GatewayIntents, RoleId, UserId};

use ecrit::{
    fields::Status,
    fields::Type,
    Ecrit
};
use fondabots_lib::command_data::{CommandData, Permission};

mod ecrit;
mod commands;
pub type DataType = fondabots_lib::DataType<Ecrit>;

fn command_checker(ctx: Context<'_, DataType, ErrType>) -> BoxFuture<Result<bool, ErrType>> {
    async move {
        let permissions = ctx.command().custom_data.downcast_ref().unwrap_or(&CommandData::default()).permission;
        let member = ctx.author_member().await;
        let auth = match member {
            Some(member) => {
                let can_thanks_to_perms = match permissions {
                    Permission::READ | Permission::NONE => true,
                    Permission::WRITE => member.roles.contains(&RoleId::new(417334522775076864)), /* Classe-C membre */
                    Permission::MANAGE => member.roles.contains(&RoleId::new(811582204790571020)) /* Équipe Critique */
                };
                can_thanks_to_perms || member.roles.contains(&RoleId::new(417333090625781761)) /* Staff */
            },
            None => false
        };
        if !auth {
            ctx.reply("Vous n'avez pas la permission d'utiliser cette commande.").await?;
        }
        Ok(auth)
    }.boxed()
}

#[tokio::main]
async fn main() {
    let args: Vec<String> = env::args().collect();
    let mut owners = HashSet::new();
    owners.insert(UserId::new(340877529973784586));

    if let Some(token) = args.get(1) {
        match Bot::default()
            .owners(owners)
            .command_checker(Box::new(command_checker))
            .set_log(725708994915860510)
            .setup(
            token.clone(),
            GatewayIntents::GUILD_MESSAGES | GatewayIntents::GUILD_MEMBERS,
            "./critibot.yml",
            commands::command_list(),
            vec![
                Affichan::new(ChannelId::new(1299620421506699275), Box::new(|ecrit| {
                    ecrit.status == Status::Ouvert || ecrit.status == Status::OuvertPlus
                })),
                Affichan::new(ChannelId::new(896361827884220467), Box::new(|ecrit| {
                    ecrit.status == Status::Inconnu || ecrit.status == Status::Infraction
                })),
                Affichan::new(ChannelId::new(896362452818747412), Box::new(|ecrit| {
                    ecrit.type_ == Type::Autre
                })),
            ],
            HashMap::new()
        ).await {
            Ok(mut bot) => if let Err(e) = bot.start().await {
                panic!("Erreur lors de l’exécution du bot: {e}");
            }
            Err(e) => panic!("Erreur lors du chargement du bot: {e}")
        }
    }
}