"""add user type

Revision ID: 783e2c4f851d
Revises: 451aa50b2ec2
Create Date: 2016-05-26 17:13:20.164928

"""

# revision identifiers, used by Alembic.
revision = '783e2c4f851d'
down_revision = '451aa50b2ec2'

from alembic import op
import sqlalchemy as sa


def upgrade():
    ### commands auto generated by Alembic - please adjust! ###
    op.add_column('users', sa.Column('user_type', sa.String(length=16), nullable=True))
    ### end Alembic commands ###


def downgrade():
    ### commands auto generated by Alembic - please adjust! ###
    op.drop_column('users', 'user_type')
    ### end Alembic commands ###
